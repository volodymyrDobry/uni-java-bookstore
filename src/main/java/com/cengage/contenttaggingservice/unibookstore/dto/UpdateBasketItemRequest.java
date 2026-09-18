package com.cengage.contenttaggingservice.unibookstore.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UpdateBasketItemRequest(
        @NotEmpty Long bookId,
        @NotNull @Positive Integer quantity
) {
}
