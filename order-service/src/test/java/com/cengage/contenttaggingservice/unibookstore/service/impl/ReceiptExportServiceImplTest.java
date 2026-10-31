package com.cengage.contenttaggingservice.unibookstore.service.impl;

import com.cengage.contenttaggingservice.unibookstore.domain.enums.OrderStatus;
import com.cengage.contenttaggingservice.unibookstore.domain.model.Ordering;
import com.cengage.contenttaggingservice.unibookstore.exception.ServiceOverloadedException;
import com.cengage.contenttaggingservice.unibookstore.repository.OrderingRepository;
import com.cengage.contenttaggingservice.unibookstore.service.ReceiptStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReceiptExportServiceImplTest {

    private static final Instant FROM = Instant.parse("2026-10-01T00:00:00Z");
    private static final Instant TO = Instant.parse("2026-10-31T00:00:00Z");

    @Mock
    private OrderingRepository orderingRepository;

    @Mock
    private ReceiptStorage receiptStorage;

    private final Executor directExecutor = Runnable::run;

    private ReceiptExportServiceImpl service(Executor executor) {
        return new ReceiptExportServiceImpl(orderingRepository, receiptStorage, executor, 2, 30L);
    }

    private Ordering readyOrder(long id, String key) {
        return Ordering.builder().id(id).customerId("alice").createdAt(Instant.now())
                .status(OrderStatus.PDF_READY).pdfLocation(key).build();
    }

    private Map<String, byte[]> unzip(byte[] zipBytes) {
        Map<String, byte[]> entries = new HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                entries.put(entry.getName(), zip.readAllBytes());
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        return entries;
    }

    @Test
    void zipsAllReadyReceiptsAndSkipsNotReadyOnes() {
        when(orderingRepository.findByCreatedAtBetweenOrderByCreatedAtAsc(FROM, TO)).thenReturn(List.of(
                readyOrder(1L, "receipts/alice/1.pdf"),
                readyOrder(2L, "receipts/alice/2.pdf"),
                Ordering.builder().id(3L).customerId("alice").createdAt(Instant.now())
                        .status(OrderStatus.PENDING).build()));
        when(receiptStorage.download("receipts/alice/1.pdf")).thenReturn(CompletableFuture.completedFuture(new byte[]{1}));
        when(receiptStorage.download("receipts/alice/2.pdf")).thenReturn(CompletableFuture.completedFuture(new byte[]{2}));

        Map<String, byte[]> entries = unzip(service(directExecutor).exportReceipts(FROM, TO));

        assertThat(entries).containsKeys("1.pdf", "2.pdf", "manifest.txt");
        assertThat(new String(entries.get("manifest.txt"), StandardCharsets.UTF_8))
                .contains("receipts=2").contains("failed=0");
    }

    @Test
    void recordsPerFileFailuresInManifestWithoutFailingTheWholeExport() {
        when(orderingRepository.findByCreatedAtBetweenOrderByCreatedAtAsc(FROM, TO)).thenReturn(List.of(
                readyOrder(1L, "receipts/alice/1.pdf"),
                readyOrder(2L, "receipts/alice/2.pdf")));
        when(receiptStorage.download("receipts/alice/1.pdf")).thenReturn(CompletableFuture.completedFuture(new byte[]{1}));
        when(receiptStorage.download("receipts/alice/2.pdf"))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("missing object")));

        Map<String, byte[]> entries = unzip(service(directExecutor).exportReceipts(FROM, TO));

        assertThat(entries).containsKeys("1.pdf", "manifest.txt");
        assertThat(entries).doesNotContainKey("2.pdf");
        assertThat(new String(entries.get("manifest.txt"), StandardCharsets.UTF_8))
                .contains("failed=1").contains("receipts/alice/2.pdf");
    }

    @Test
    void rejectedExecutionIsTranslatedToServiceOverloadedNotA500() {
        when(orderingRepository.findByCreatedAtBetweenOrderByCreatedAtAsc(FROM, TO))
                .thenReturn(List.of(readyOrder(1L, "receipts/alice/1.pdf")));
        Executor saturated = runnable -> {
            throw new RejectedExecutionException("pool and queue are full");
        };

        assertThatThrownBy(() -> service(saturated).exportReceipts(FROM, TO))
                .isInstanceOf(ServiceOverloadedException.class);
    }

    @Test
    void emptyPeriodProducesZipWithOnlyManifest() {
        when(orderingRepository.findByCreatedAtBetweenOrderByCreatedAtAsc(FROM, TO)).thenReturn(List.of());

        Map<String, byte[]> entries = unzip(service(directExecutor).exportReceipts(FROM, TO));

        assertThat(entries).containsOnlyKeys("manifest.txt");
        assertThat(new String(entries.get("manifest.txt"), StandardCharsets.UTF_8)).contains("receipts=0");
    }
}
