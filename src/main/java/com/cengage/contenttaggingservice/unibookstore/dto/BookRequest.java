package com.cengage.contenttaggingservice.unibookstore.dto;

import com.cengage.contenttaggingservice.unibookstore.domain.enums.Genre;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BookRequest(@NotBlank String title, @NotBlank String description, @NotBlank String author,
                          @NotNull Genre genre, @NotBlank String imageUrl) {
}
