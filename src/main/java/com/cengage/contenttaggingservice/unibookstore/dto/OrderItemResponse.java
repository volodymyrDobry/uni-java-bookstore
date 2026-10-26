package com.cengage.contenttaggingservice.unibookstore.dto;

public record OrderItemResponse(
        Long bookId,
        Integer count
) {
}
