package com.cengage.contenttaggingservice.unibookstore.dto;

import com.cengage.contenttaggingservice.unibookstore.domain.enums.Genre;

public record UpdateBookDetailsRequest(
        String title,
        String description,
        String author,
        Genre genre,
        String imageUrl,
        Boolean enabled
) {
}
