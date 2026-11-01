package com.cengage.contenttaggingservice.unibookstore.messaging;

import java.time.Instant;
import java.util.List;

/**
 * Sent by order-service when an order is placed, asking book-management-service
 * to reserve and remove stock for every line.
 */
public record ReserveStockCommand(
        Long orderId,
        String customerId,
        Instant createdAt,
        List<ReserveStockLine> lines
) {
}
