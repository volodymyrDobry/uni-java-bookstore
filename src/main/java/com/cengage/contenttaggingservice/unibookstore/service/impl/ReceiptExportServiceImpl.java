package com.cengage.contenttaggingservice.unibookstore.service.impl;

import com.cengage.contenttaggingservice.unibookstore.config.AsyncConfig;
import com.cengage.contenttaggingservice.unibookstore.domain.enums.OrderStatus;
import com.cengage.contenttaggingservice.unibookstore.domain.model.Ordering;
import com.cengage.contenttaggingservice.unibookstore.exception.ServiceOverloadedException;
import com.cengage.contenttaggingservice.unibookstore.repository.OrderingRepository;
import com.cengage.contenttaggingservice.unibookstore.service.ReceiptExportService;
import com.cengage.contenttaggingservice.unibookstore.service.ReceiptStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
public class ReceiptExportServiceImpl implements ReceiptExportService {

    private final OrderingRepository orderingRepository;
    private final ReceiptStorage receiptStorage;
    private final Executor executor;
    private final int batchSize;
    private final long timeoutSeconds;

    public ReceiptExportServiceImpl(OrderingRepository orderingRepository,
                                    ReceiptStorage receiptStorage,
                                    @Qualifier(AsyncConfig.ORDERING_EXECUTOR) Executor executor,
                                    @Value("${app.async.export-batch-size:4}") int batchSize,
                                    @Value("${app.async.export-timeout-seconds:30}") long timeoutSeconds) {
        this.orderingRepository = orderingRepository;
        this.receiptStorage = receiptStorage;
        this.executor = executor;
        this.batchSize = batchSize;
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public byte[] exportReceipts(Instant from, Instant to) {
        List<String> keys = orderingRepository.findByCreatedAtBetweenOrderByCreatedAtAsc(from, to).stream()
                .filter(order -> order.getStatus() == OrderStatus.PDF_READY && order.getPdfLocation() != null)
                .map(Ordering::getPdfLocation)
                .toList();

        CompletableFuture<byte[]> export = downloadAllBatched(keys)
                .thenCompose(this::buildZip)
                .orTimeout(timeoutSeconds, TimeUnit.SECONDS);

        return await(export);
    }

    private CompletableFuture<List<DownloadedReceipt>> downloadAllBatched(List<String> keys) {
        CompletableFuture<List<DownloadedReceipt>> accumulator =
                CompletableFuture.completedFuture(new ArrayList<>());

        for (int start = 0; start < keys.size(); start += batchSize) {
            List<String> batch = keys.subList(start, Math.min(start + batchSize, keys.size()));
            accumulator = accumulator.thenCompose(collected ->
                    downloadBatch(batch).thenApply(part -> {
                        collected.addAll(part);
                        return collected;
                    }));
        }
        return accumulator;
    }

    private CompletableFuture<List<DownloadedReceipt>> downloadBatch(List<String> batch) {
        List<CompletableFuture<DownloadedReceipt>> futures = batch.stream()
                .map(key -> CompletableFuture.supplyAsync(() -> fetchOne(key), executor))
                .toList();
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                .thenApply(ignored -> futures.stream().map(CompletableFuture::join).toList());
    }

    private DownloadedReceipt fetchOne(String key) {
        try {
            byte[] content = receiptStorage.download(key).join();
            return new DownloadedReceipt(key, content, null);
        } catch (RuntimeException ex) {
            log.warn("Failed to download receipt {} for export: {}", key, ex.getMessage());
            return new DownloadedReceipt(key, null, ex.getMessage());
        }
    }

    private CompletableFuture<byte[]> buildZip(List<DownloadedReceipt> receipts) {
        return CompletableFuture.supplyAsync(() -> zip(receipts), executor);
    }

    private byte[] zip(List<DownloadedReceipt> receipts) {
        List<String> errors = new ArrayList<>();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream(); ZipOutputStream zip = new ZipOutputStream(out)) {
            for (DownloadedReceipt receipt : receipts) {
                if (receipt.content() == null) {
                    errors.add(receipt.key() + " -> " + receipt.error());
                    continue;
                }
                zip.putNextEntry(new ZipEntry(entryName(receipt.key())));
                zip.write(receipt.content());
                zip.closeEntry();
            }
            zip.putNextEntry(new ZipEntry("manifest.txt"));
            String manifest = "receipts=" + receipts.size() + System.lineSeparator()
                    + "failed=" + errors.size() + System.lineSeparator()
                    + String.join(System.lineSeparator(), errors);
            zip.write(manifest.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.finish();
            return out.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to build receipts ZIP", ex);
        }
    }

    private String entryName(String key) {
        int slash = key.lastIndexOf('/');
        return slash >= 0 ? key.substring(slash + 1) : key;
    }

    private byte[] await(CompletableFuture<byte[]> export) {
        try {
            return export.join();
        } catch (CompletionException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof RejectedExecutionException) {
                throw new ServiceOverloadedException("Export rejected: server busy, narrow the period or try again later");
            }
            if (cause instanceof TimeoutException) {
                throw new ServiceOverloadedException("Export timed out: narrow the period or try again later");
            }
            throw ex;
        }
    }

    private record DownloadedReceipt(String key, byte[] content, String error) {
    }
}
