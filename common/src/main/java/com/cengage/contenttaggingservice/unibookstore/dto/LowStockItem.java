package com.cengage.contenttaggingservice.unibookstore.dto;

public record LowStockItem(
        Long bookId,
        String title,
        Integer quantity
) {
}
