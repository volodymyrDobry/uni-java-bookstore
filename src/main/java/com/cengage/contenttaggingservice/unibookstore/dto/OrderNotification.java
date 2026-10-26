package com.cengage.contenttaggingservice.unibookstore.dto;

import java.math.BigDecimal;

public record OrderNotification(
        Long orderId,
        String customerId,
        int itemCount,
        BigDecimal total
) {
}
