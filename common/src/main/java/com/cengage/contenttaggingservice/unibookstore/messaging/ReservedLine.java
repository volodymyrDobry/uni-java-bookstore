package com.cengage.contenttaggingservice.unibookstore.messaging;

import java.math.BigDecimal;

/** A reserved book line with the price captured at reservation time. */
public record ReservedLine(
        Long bookId,
        String title,
        String author,
        int quantity,
        BigDecimal unitPrice
) {
}
