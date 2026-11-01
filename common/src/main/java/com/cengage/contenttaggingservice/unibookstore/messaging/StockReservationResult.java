package com.cengage.contenttaggingservice.unibookstore.messaging;

import java.time.Instant;
import java.util.List;

/**
 * The single reply book-management-service sends for a {@link ReserveStockCommand}.
 * It reports the outcome of making the order: stock removed ({@code RESERVED}, with
 * priced {@code lines}), not enough stock ({@code INSUFFICIENT_STOCK}, with
 * {@code shortfalls}), or an unexpected failure ({@code ERROR}, with a {@code message}).
 */
public record StockReservationResult(
        Long orderId,
        String customerId,
        Instant createdAt,
        ReservationStatus status,
        List<ReservedLine> lines,
        List<ShortfallLine> shortfalls,
        String message
) {

    public static StockReservationResult reserved(Long orderId, String customerId, Instant createdAt,
                                                  List<ReservedLine> lines) {
        return new StockReservationResult(orderId, customerId, createdAt,
                ReservationStatus.RESERVED, List.copyOf(lines), List.of(), null);
    }

    public static StockReservationResult insufficientStock(Long orderId, String customerId, Instant createdAt,
                                                           List<ShortfallLine> shortfalls) {
        return new StockReservationResult(orderId, customerId, createdAt,
                ReservationStatus.INSUFFICIENT_STOCK, List.of(), List.copyOf(shortfalls), "Insufficient stock");
    }

    public static StockReservationResult error(Long orderId, String customerId, Instant createdAt, String message) {
        return new StockReservationResult(orderId, customerId, createdAt,
                ReservationStatus.ERROR, List.of(), List.of(), message);
    }
}
