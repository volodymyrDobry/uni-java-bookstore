# Frontend task: Ordering, receipts & admin order stream

You are implementing the frontend for a new **Ordering** feature in the Uni Book Store app.
The backend is already built. Below is everything you need: the functionality, the exact API
contracts, the real-time channel (SSE), and what to build. Implement the UI and client calls to
match these contracts.

## Context

- **Base URL:** `http://localhost:8081`, all endpoints under `/api/v1`.
- **Auth:** OAuth2 / Keycloak. Every request needs `Authorization: Bearer <access_token>`.
  - Realm `uni-book-store`, public client `uni-book-store-client`.
  - Token endpoint: `POST http://localhost:8080/realms/uni-book-store/protocol/openid-connect/token`
    (`grant_type=password`, `client_id=uni-book-store-client`, `username`, `password`).
  - Roles: **USER** (can order) and **ADMIN** (can watch the order stream and export receipts).
- **Async model to understand:** placing an order is accepted immediately and returns an order in
  `PENDING` status with `pdfLocation = null`. The PDF receipt is generated and uploaded to storage
  **in the background**. The order later becomes `PDF_READY` (downloadable) or `FAILED`. The UI must
  reflect this (poll or refresh; enable download only when `PDF_READY`).

## Data shapes (JSON)

```jsonc
// OrderResponse
{
  "id": 100,
  "customerId": "user",
  "createdAt": "2026-10-18T12:34:56.789Z",   // ISO-8601 instant (UTC)
  "status": "PENDING",                         // "PENDING" | "PDF_READY" | "FAILED"
  "pdfLocation": null,                          // storage key, null until PDF_READY
  "items": [ { "bookId": 1, "count": 2 } ]
}

// OrderItemResponse
{ "bookId": 1, "count": 2 }

// OrderNotification  (payload of the SSE "order" event)
{ "orderId": 100, "customerId": "user", "itemCount": 2, "total": 19.98 }
```

Error responses are JSON with at least `status` and a message field (Spring `ProblemDetail` for
404/400: `{ "status": 404, "detail": "..." }`; default error body for 409/403/503:
`{ "status": 409, "error": "...", "path": "..." }`). Treat any non-2xx as an error and surface the
status.

## Endpoints / contracts

### USER — Orders

| Method | Path | Auth | Request | Success | Errors |
|---|---|---|---|---|---|
| POST | `/api/v1/orders` | USER | *(no body)* — orders the current user's basket | `202 Accepted` → `OrderResponse` (status `PENDING`) | `409` empty basket / insufficient stock |
| GET | `/api/v1/orders` | USER | — | `200` → `OrderResponse[]` (newest first) | — |
| GET | `/api/v1/orders/{id}/receipt` | USER (owner only) | — | `200` → PDF binary, `Content-Type: application/pdf`, `Content-Disposition: attachment; filename="order-{id}.pdf"` | `404` not ready / not found, `403` not owner |

Notes:
- Placing an order consumes the basket (basket is cleared) and decrements stock server-side.
- `409` on POST means the basket is empty or a book doesn't have enough stock — show a clear message
  and keep the user on the cart.
- The receipt download only works once the order's `status === "PDF_READY"`. Before that, the GET
  returns `404` with "Receipt is not ready yet".

### Pre-requisite (existing endpoints you'll use)

The basket already exists. To test ordering, add items first:
- `POST /api/v1/basket` body `{ "bookId": number, "quantity": number }`
- `GET /api/v1/basket` returns the current basket.

### ADMIN — Live order stream (SSE)

| Method | Path | Auth | Response |
|---|---|---|---|
| GET | `/api/v1/admin/orders/stream` | ADMIN | `text/event-stream` (SSE) |

- The server pushes an event every time **any** user places an order.
- Event name: `order`. Event `data` is an `OrderNotification` JSON object (see above).
- **Important client constraint:** the native `EventSource` API cannot send an `Authorization`
  header. Since this endpoint requires a Bearer token, use a fetch-based SSE client that supports
  headers — e.g. [`@microsoft/fetch-event-source`](https://github.com/Azure/fetch-event-source) —
  and pass `Authorization: Bearer <admin_token>`. Reconnect on drop; the connection is long-lived.
- Use it to show a live toast / badge ("New order #100 — 2 items, $19.98") on admin screens.

### ADMIN — Export receipts by period (ZIP)

| Method | Path | Auth | Query | Success | Errors |
|---|---|---|---|---|---|
| GET | `/api/v1/admin/orders/receipts/export` | ADMIN | `from`, `to` = ISO-8601 instants (e.g. `2026-10-01T00:00:00Z`) | `200` → ZIP binary, `Content-Type: application/zip`, `Content-Disposition: attachment; filename="receipts-<from>-<to>.zip"` | `503` server busy / export timed out |

- The ZIP contains one `*.pdf` per ready receipt in the window, plus a `manifest.txt`
  (`receipts=`, `failed=`, and any per-file errors). Individual failed files don't fail the export.
- `503` (`ServiceOverloadedException`) means the server is overloaded or the export timed out — show
  "Server busy, narrow the date range or try again later". **Never treat 503 as a crash.**

## What to build

1. **Checkout / Place order (USER)**
   - A "Place order" button on the basket/cart. On click → `POST /api/v1/orders`.
   - On `202`: show success, navigate to Orders, and show the new order as `PENDING`.
   - On `409`: show the returned message (empty basket / not enough stock); don't clear the cart.

2. **My orders (USER)**
   - List from `GET /api/v1/orders`. Show id, date (`createdAt`), item count/total if you compute it,
     and a **status badge**: `PENDING` (spinner/"preparing receipt"), `PDF_READY` (green),
     `FAILED` (red).
   - **Download receipt** button, enabled only when `PDF_READY` → `GET /api/v1/orders/{id}/receipt`,
     save the returned PDF (respect the `Content-Disposition` filename).
   - Because the receipt is async, either poll `GET /api/v1/orders` every few seconds while any order
     is `PENDING`, or give a manual "Refresh" control. Stop polling once none are `PENDING`.

3. **Admin live notifications (ADMIN)**
   - Subscribe to `GET /api/v1/admin/orders/stream` (fetch-based SSE with auth header).
   - For each `order` event, show a toast/notification and optionally increment an "orders today"
     counter. Handle reconnect.

4. **Admin receipts export (ADMIN)**
   - A form with a date range (from/to). Convert the selected dates to ISO-8601 UTC instants.
   - Submit → `GET /api/v1/admin/orders/receipts/export?from=...&to=...`, download the ZIP.
   - Handle `503` with a friendly "try a smaller range / later" message and a retry affordance.

## Status codes to handle (summary)

- `202` order accepted (PENDING).
- `200` list / receipt PDF / export ZIP.
- `400` validation error.
- `403` not the owner of the order (receipt).
- `404` order not found or receipt not ready yet.
- `409` empty basket / insufficient stock.
- `503` server overloaded or export timed out (retry later).
- `401` missing/expired token → trigger re-login.

## Acceptance checklist

- [ ] USER can place an order from a non-empty basket and sees it as `PENDING`.
- [ ] Orders list reflects status transitions `PENDING → PDF_READY` (via poll/refresh).
- [ ] Download button works only for `PDF_READY` and saves `order-{id}.pdf`.
- [ ] `409` on empty/insufficient basket is shown clearly, cart preserved.
- [ ] ADMIN sees a live toast per new order over SSE (with auth header, auto-reconnect).
- [ ] ADMIN can export a date range and download the ZIP; `503` shows a retry message.
- [ ] All requests send the Bearer token; `401` triggers re-login.
