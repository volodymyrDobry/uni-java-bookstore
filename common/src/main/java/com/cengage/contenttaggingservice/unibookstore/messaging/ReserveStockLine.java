package com.cengage.contenttaggingservice.unibookstore.messaging;

public record ReserveStockLine(
        Long bookId,
        int quantity
) {
}
