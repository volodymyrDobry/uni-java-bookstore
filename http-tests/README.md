# HTTP tests for the custom AOP annotations

Manual request files (IntelliJ HTTP Client format) that exercise the three custom
annotations added in this project: `@BookOwner`, `@Auditable`, `@TimeTracker`.

## Prerequisites
- App running on `http://localhost:8081`, Keycloak on `http://localhost:8080`.
- Keycloak realm `uni-book-store`, public client `uni-book-store-client` with
  **Direct Access Grants enabled** (the resource-owner-password / "explicit" flow).
- The realm's `sslRequired` must allow plain HTTP for local runs (set it to `none`;
  the default `external` makes Keycloak reject HTTP requests coming through Docker with
  `"HTTPS required"`).
- All URLs are hard-coded in the `.http` files (no environment file needed).

## Users
| username | password | role  |
|----------|----------|-------|
| admin    | admin    | ADMIN |
| user     | user     | USER  |
| vova     | vova     | ADMIN |

## Files
- `00-auth.http` — fetches a token for each user (optional convenience; stores globals).
- `book-owner.http` — **`@BookOwner`**, the only annotation with an observable HTTP result:
  owner → `200`, non-owner → `403`. Fully self-checking via `client.test(...)`.
- `auditable.http` — **`@Auditable`**: triggers `BOOK_CREATED` / `BOOK_UPDATED`; verify the
  `AUDIT ... status=SUCCESS` lines in the app console.
- `time-tracker.http` — **`@TimeTracker`**: triggers the `@Around` timing; verify the
  `Starting timeTracker` / `Method has finished` lines in the app console.

Each functional file is self-contained (it logs in first), so you can run any file on its own.

## What is / isn't checked by the request itself
- `@BookOwner`: fully asserted (HTTP status 200 vs 403).
- `@Auditable` and `@TimeTracker`: effects are **log lines**, not HTTP responses, so the
  requests assert the endpoint succeeds and you confirm the log output. Their success/failure
  branches are unit-tested in `AuditAspectTest` / `CRUDBookServiceAuditAspectTest`.
