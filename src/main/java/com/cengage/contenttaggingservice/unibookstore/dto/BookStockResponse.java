package com.cengage.contenttaggingservice.unibookstore.dto;

import java.math.BigDecimal;

public record BookStockResponse(
        Long bookId,
        BigDecimal price,
        Integer quantity
) {
}
