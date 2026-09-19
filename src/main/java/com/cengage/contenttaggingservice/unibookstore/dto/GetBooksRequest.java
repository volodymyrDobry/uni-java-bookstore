package com.cengage.contenttaggingservice.unibookstore.dto;

import com.cengage.contenttaggingservice.unibookstore.domain.enums.Genre;

import java.math.BigDecimal;

public record GetBooksRequest(
        String title,
        String author,
        Genre genre,
        Boolean enabled,
        BigDecimal minPrice,
        BigDecimal maxPrice
) {
}
