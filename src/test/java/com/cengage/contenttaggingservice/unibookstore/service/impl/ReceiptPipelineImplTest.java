package com.cengage.contenttaggingservice.unibookstore.service.impl;

import com.cengage.contenttaggingservice.unibookstore.dto.OrderLine;
import com.cengage.contenttaggingservice.unibookstore.dto.OrderSnapshot;
import com.cengage.contenttaggingservice.unibookstore.service.PdfReceiptGenerator;
import com.cengage.contenttaggingservice.unibookstore.service.ReceiptAssembler;
import com.cengage.contenttaggingservice.unibookstore.service.ReceiptStorage;
import com.cengage.contenttaggingservice.unibookstore.service.StorefrontClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReceiptPipelineImplTest {

    @Mock
    private ReceiptAssembler assembler;

    @Mock
    private StorefrontClient storefront;

    @Mock
    private PdfReceiptGenerator pdfGenerator;

    @Mock
    private ReceiptStorage storage;

    @Mock
    private OrderStatusWriter statusWriter;

    private final OrderSnapshot snapshot = new OrderSnapshot(
            1L, "alice", Instant.now(),
            List.of(new OrderLine(1L, "Dune", "Herbert", 1, BigDecimal.TEN)), BigDecimal.TEN);

    private ReceiptPipelineImpl pipeline(long timeoutSeconds) {
        return new ReceiptPipelineImpl(assembler, storefront, pdfGenerator, storage, statusWriter, timeoutSeconds);
    }

    private void stubAssembleAndRender() {
        when(assembler.buildModel(snapshot)).thenReturn(CompletableFuture.completedFuture(snapshot));
        when(storefront.fetchTagline()).thenReturn(CompletableFuture.completedFuture("hi"));
        when(pdfGenerator.render(eq(snapshot), eq("hi"))).thenReturn(new byte[]{1, 2, 3});
    }

    @Test
    void combinesInputsUploadsAndMarksReadyOnSuccess() {
        stubAssembleAndRender();
        when(storage.upload(anyString(), any(), eq("application/pdf")))
                .thenReturn(CompletableFuture.completedFuture("receipts/alice/1.pdf"));

        pipeline(15).generateAndStore(snapshot);

        verify(statusWriter, timeout(1000)).markReady(1L, "receipts/alice/1.pdf");
        verify(statusWriter, never()).markFailed(any());
    }

    @Test
    void uploadFailureMarksFailedAndDoesNotCrashCaller() {
        stubAssembleAndRender();
        when(storage.upload(anyString(), any(), eq("application/pdf")))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("s3 down")));

        assertThatCode(() -> pipeline(15).generateAndStore(snapshot)).doesNotThrowAnyException();

        verify(statusWriter, timeout(1000)).markFailed(1L);
        verify(statusWriter, never()).markReady(any(), any());
    }

    @Test
    void timeoutMarksFailed() {
        stubAssembleAndRender();
        when(storage.upload(anyString(), any(), eq("application/pdf")))
                .thenReturn(new CompletableFuture<>());

        pipeline(1).generateAndStore(snapshot);

        verify(statusWriter, timeout(3000)).markFailed(1L);
    }

    @Test
    void returnsImmediatelyWithoutBlockingTheCaller() {
        stubAssembleAndRender();
        when(storage.upload(anyString(), any(), eq("application/pdf")))
                .thenReturn(new CompletableFuture<>());

        long start = System.nanoTime();
        pipeline(30).generateAndStore(snapshot);
        long elapsedMillis = (System.nanoTime() - start) / 1_000_000;

        assertThat(elapsedMillis).isLessThan(500);
    }

    @Test
    void generateAndStoreSyncReturnsStoredKey() {
        stubAssembleAndRender();
        when(storage.upload(anyString(), any(), eq("application/pdf")))
                .thenReturn(CompletableFuture.completedFuture("receipts/alice/1.pdf"));

        assertThat(pipeline(15).generateAndStoreSync(snapshot)).isEqualTo("receipts/alice/1.pdf");
    }

    @Test
    void generateAndStoreSyncPropagatesFailure() {
        stubAssembleAndRender();
        when(storage.upload(anyString(), any(), eq("application/pdf")))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("s3 down")));

        assertThatThrownBy(() -> pipeline(15).generateAndStoreSync(snapshot))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("s3 down");
    }
}
