package com.cengage.contenttaggingservice.unibookstore.service.impl;

import com.cengage.contenttaggingservice.unibookstore.domain.model.LowStockDigest;
import com.cengage.contenttaggingservice.unibookstore.dto.LowStockDigestResponse;
import com.cengage.contenttaggingservice.unibookstore.dto.LowStockItem;
import com.cengage.contenttaggingservice.unibookstore.mapper.LowStockDigestMapper;
import com.cengage.contenttaggingservice.unibookstore.repository.LowStockDigestRepository;
import com.cengage.contenttaggingservice.unibookstore.service.LowStockService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LowStockDigestServiceImplTest {

    @Mock
    private LowStockService lowStockService;

    @Mock
    private LowStockDigestRepository lowStockDigestRepository;

    @Mock
    private LowStockDigestMapper lowStockDigestMapper;

    @InjectMocks
    private LowStockDigestServiceImpl service;

    @Test
    void recordDigestStoresWhatTheStockQueryReturnsAndMapsTheSavedEntity() {
        List<LowStockItem> items = List.of(new LowStockItem(1L, "Dune", 2), new LowStockItem(2L, "1984", 4));
        when(lowStockService.findLowStock(5, true)).thenReturn(items);
        when(lowStockDigestRepository.save(any(LowStockDigest.class))).thenAnswer(invocation -> {
            LowStockDigest digest = invocation.getArgument(0);
            digest.setId(100L);
            return digest;
        });
        LowStockDigestResponse mapped = new LowStockDigestResponse(100L, Instant.now(), 5, true, "SSE", 2, List.of());
        when(lowStockDigestMapper.toResponse(any(LowStockDigest.class))).thenReturn(mapped);

        LowStockDigestResponse response = service.recordDigest(5, true, "SSE");

        assertThat(response).isSameAs(mapped);

        ArgumentCaptor<LowStockDigest> captor = ArgumentCaptor.forClass(LowStockDigest.class);
        verify(lowStockDigestRepository).save(captor.capture());
        LowStockDigest saved = captor.getValue();
        assertThat(saved.getThreshold()).isEqualTo(5);
        assertThat(saved.isOnlyEnabled()).isTrue();
        assertThat(saved.getChannel()).isEqualTo("SSE");
        assertThat(saved.getItemCount()).isEqualTo(2);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getItems())
                .extracting("bookId", "title", "quantity", "digest")
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(1L, "Dune", 2, saved),
                        org.assertj.core.groups.Tuple.tuple(2L, "1984", 4, saved));
    }

    @Test
    void recordDigestStoresAnEmptyDigestWhenNothingIsLow() {
        when(lowStockService.findLowStock(5, true)).thenReturn(List.of());
        when(lowStockDigestRepository.save(any(LowStockDigest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(lowStockDigestMapper.toResponse(any(LowStockDigest.class)))
                .thenReturn(new LowStockDigestResponse(1L, Instant.now(), 5, true, "LOG", 0, List.of()));

        service.recordDigest(5, true, "LOG");

        ArgumentCaptor<LowStockDigest> captor = ArgumentCaptor.forClass(LowStockDigest.class);
        verify(lowStockDigestRepository).save(captor.capture());
        assertThat(captor.getValue().getItemCount()).isZero();
        assertThat(captor.getValue().getItems()).isEmpty();
    }

    @Test
    void recentDigestsReadsNewestFirstAndMapsEachEntity() {
        LowStockDigest digest = LowStockDigest.builder().id(1L).createdAt(Instant.now())
                .threshold(5).onlyEnabled(true).channel("LOG").itemCount(0).build();
        when(lowStockDigestRepository.findByOrderByCreatedAtDescIdDesc(any(Pageable.class)))
                .thenReturn(List.of(digest));
        LowStockDigestResponse mapped = new LowStockDigestResponse(1L, digest.getCreatedAt(), 5, true, "LOG", 0, List.of());
        when(lowStockDigestMapper.toResponse(digest)).thenReturn(mapped);

        assertThat(service.recentDigests(20)).containsExactly(mapped);

        ArgumentCaptor<Pageable> pageCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(lowStockDigestRepository).findByOrderByCreatedAtDescIdDesc(pageCaptor.capture());
        assertThat(pageCaptor.getValue()).isEqualTo(PageRequest.of(0, 20));
    }
}
