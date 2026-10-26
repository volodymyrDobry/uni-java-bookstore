package com.cengage.contenttaggingservice.unibookstore.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderSnapshot(
        Long orderId,
        String customerId,
        Instant createdAt,
        List<OrderLine> lines,
        BigDecimal grandTotal
) {
}
