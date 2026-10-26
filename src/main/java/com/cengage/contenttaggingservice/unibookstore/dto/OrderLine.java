package com.cengage.contenttaggingservice.unibookstore.dto;

import java.math.BigDecimal;

public record OrderLine(
        Long bookId,
        String title,
        String author,
        int count,
        BigDecimal unitPrice
) {
}
