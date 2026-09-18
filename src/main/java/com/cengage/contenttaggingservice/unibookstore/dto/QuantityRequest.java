package com.cengage.contenttaggingservice.unibookstore.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record QuantityRequest(@NotNull @Positive Integer quantity) {
}
