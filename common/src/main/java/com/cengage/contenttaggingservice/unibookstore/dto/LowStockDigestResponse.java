package com.cengage.contenttaggingservice.unibookstore.dto;

import java.time.Instant;
import java.util.List;

/** A stored low-stock digest as returned to the admin UI. */
public record LowStockDigestResponse(
        Long id,
        Instant createdAt,
        int threshold,
        boolean onlyEnabled,
        String channel,
        int itemCount,
        List<LowStockItem> items
) {
}
