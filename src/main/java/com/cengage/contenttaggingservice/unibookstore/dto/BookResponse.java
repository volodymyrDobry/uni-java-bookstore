package com.cengage.contenttaggingservice.unibookstore.dto;

import com.cengage.contenttaggingservice.unibookstore.domain.enums.Genre;

import java.math.BigDecimal;

public record BookResponse(Long id, String title, String description, String author, Genre genre, String imageUrl,
                           boolean enabled, Integer quantity, BigDecimal price) {
}
