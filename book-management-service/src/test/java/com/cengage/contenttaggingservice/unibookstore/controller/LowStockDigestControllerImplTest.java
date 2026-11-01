package com.cengage.contenttaggingservice.unibookstore.controller;

import com.cengage.contenttaggingservice.unibookstore.dto.LowStockDigestResponse;
import com.cengage.contenttaggingservice.unibookstore.service.LowStockDigestService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LowStockDigestControllerImplTest {

    @Mock
    private LowStockDigestService lowStockDigestService;

    @InjectMocks
    private LowStockDigestControllerImpl controller;

    private LowStockDigestResponse digest() {
        return new LowStockDigestResponse(1L, Instant.now(), 5, true, "MANUAL", 0, List.of());
    }

    @Test
    void recentDigestsDelegatesWithTheRequestedLimit() {
        List<LowStockDigestResponse> digests = List.of(digest());
        when(lowStockDigestService.recentDigests(20)).thenReturn(digests);

        assertThat(controller.recentDigests(20)).isSameAs(digests);
        verify(lowStockDigestService).recentDigests(20);
    }

    @Test
    void generateDigestRecordsOnTheManualChannel() {
        LowStockDigestResponse created = digest();
        when(lowStockDigestService.recordDigest(10, true, "MANUAL")).thenReturn(created);

        assertThat(controller.generateDigest(10, true)).isSameAs(created);
        verify(lowStockDigestService).recordDigest(10, true, "MANUAL");
    }
}
