package com.cengage.contenttaggingservice.unibookstore.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record StockRequest(
        @PositiveOrZero int quantity,
        @DecimalMin(value = "0.0", inclusive = false) BigDecimal price
) {
}
