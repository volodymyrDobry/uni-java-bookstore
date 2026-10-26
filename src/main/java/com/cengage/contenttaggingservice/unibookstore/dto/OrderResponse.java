package com.cengage.contenttaggingservice.unibookstore.dto;

import com.cengage.contenttaggingservice.unibookstore.domain.enums.OrderStatus;

import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        String customerId,
        Instant createdAt,
        OrderStatus status,
        String pdfLocation,
        List<OrderItemResponse> items
) {
}
