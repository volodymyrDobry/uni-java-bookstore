package com.cengage.contenttaggingservice.unibookstore.dto;

import com.cengage.contenttaggingservice.unibookstore.domain.enums.Genre;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record CreateBookRequest(
        @NotBlank String title,
        @NotBlank String description,
        @NotBlank String author,
        @NotNull Genre genre,
        @NotBlank String imageUrl,
        boolean enabled,
        @DecimalMin("0.0") BigDecimal price,
        @PositiveOrZero int quantity
) {
}
