package com.cengage.contenttaggingservice.unibookstore.dto;

public record BasketItemResponse(
        Long bookId,
        String title,
        Integer quantity
) {
}
