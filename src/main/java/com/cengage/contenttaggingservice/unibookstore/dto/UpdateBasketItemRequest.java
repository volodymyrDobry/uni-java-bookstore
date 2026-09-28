package com.cengage.contenttaggingservice.unibookstore.dto;

import com.cengage.contenttaggingservice.unibookstore.annotation.BookExists;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UpdateBasketItemRequest(
        @BookExists @NotNull Long bookId,
        @NotNull @Positive Integer quantity
) {
}
