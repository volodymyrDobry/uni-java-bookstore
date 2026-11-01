package com.cengage.contenttaggingservice.unibookstore.mapper;

import com.cengage.contenttaggingservice.unibookstore.domain.model.LowStockDigest;
import com.cengage.contenttaggingservice.unibookstore.domain.model.LowStockDigestItem;
import com.cengage.contenttaggingservice.unibookstore.dto.LowStockDigestResponse;
import com.cengage.contenttaggingservice.unibookstore.dto.LowStockItem;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class LowStockDigestMapperTest {

    private final LowStockDigestMapper mapper = new LowStockDigestMapperImpl();

    @Test
    void toResponseMapsAllFieldsAndItems() {
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        LowStockDigest digest = LowStockDigest.builder()
                .id(7L)
                .createdAt(createdAt)
                .threshold(5)
                .onlyEnabled(true)
                .channel("SSE")
                .itemCount(1)
                .build();
        digest.addItem(LowStockDigestItem.builder().bookId(1L).title("Dune").quantity(2).build());

        LowStockDigestResponse response = mapper.toResponse(digest);

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.createdAt()).isEqualTo(createdAt);
        assertThat(response.threshold()).isEqualTo(5);
        assertThat(response.onlyEnabled()).isTrue();
        assertThat(response.channel()).isEqualTo("SSE");
        assertThat(response.itemCount()).isEqualTo(1);
        assertThat(response.items()).containsExactly(new LowStockItem(1L, "Dune", 2));
    }

    @Test
    void toResponseMapsAnEmptyDigest() {
        LowStockDigest digest = LowStockDigest.builder()
                .id(1L).createdAt(Instant.now()).threshold(10).onlyEnabled(false).channel("LOG").itemCount(0).build();

        LowStockDigestResponse response = mapper.toResponse(digest);

        assertThat(response.items()).isEmpty();
        assertThat(response.onlyEnabled()).isFalse();
    }
}
