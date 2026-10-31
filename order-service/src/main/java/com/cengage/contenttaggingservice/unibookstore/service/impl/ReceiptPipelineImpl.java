package com.cengage.contenttaggingservice.unibookstore.service.impl;

import com.cengage.contenttaggingservice.unibookstore.dto.OrderSnapshot;
import com.cengage.contenttaggingservice.unibookstore.service.PdfReceiptGenerator;
import com.cengage.contenttaggingservice.unibookstore.service.ReceiptAssembler;
import com.cengage.contenttaggingservice.unibookstore.service.ReceiptPipeline;
import com.cengage.contenttaggingservice.unibookstore.service.ReceiptStorage;
import com.cengage.contenttaggingservice.unibookstore.service.StorefrontClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ReceiptPipelineImpl implements ReceiptPipeline {

    private final ReceiptAssembler receiptAssembler;
    private final StorefrontClient storefrontClient;
    private final PdfReceiptGenerator pdfReceiptGenerator;
    private final ReceiptStorage receiptStorage;
    private final OrderStatusWriter orderStatusWriter;
    private final long timeoutSeconds;

    public ReceiptPipelineImpl(ReceiptAssembler receiptAssembler,
                               StorefrontClient storefrontClient,
                               PdfReceiptGenerator pdfReceiptGenerator,
                               ReceiptStorage receiptStorage,
                               OrderStatusWriter orderStatusWriter,
                               @Value("${app.async.receipt-timeout-seconds:15}") long timeoutSeconds) {
        this.receiptAssembler = receiptAssembler;
        this.storefrontClient = storefrontClient;
        this.pdfReceiptGenerator = pdfReceiptGenerator;
        this.receiptStorage = receiptStorage;
        this.orderStatusWriter = orderStatusWriter;
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public void generateAndStore(OrderSnapshot snapshot) {
        buildAndUpload(snapshot)
                .whenComplete((storedKey, ex) -> {
                    if (ex == null) {
                        orderStatusWriter.markReady(snapshot.orderId(), storedKey);
                        log.info("Receipt stored for order {} at {}", snapshot.orderId(), storedKey);
                    } else {
                        orderStatusWriter.markFailed(snapshot.orderId());
                        log.error("Receipt pipeline failed for order {}: {}", snapshot.orderId(), ex.getMessage(), ex);
                    }
                });
    }

    @Override
    public String generateAndStoreSync(OrderSnapshot snapshot) {
        try {
            return buildAndUpload(snapshot).join();
        } catch (CompletionException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new IllegalStateException("Receipt generation failed for order " + snapshot.orderId(), cause);
        }
    }

    private CompletableFuture<String> buildAndUpload(OrderSnapshot snapshot) {
        String key = "receipts/" + snapshot.customerId() + "/" + snapshot.orderId() + ".pdf";
        return receiptAssembler.buildModel(snapshot)
                .thenCombine(storefrontClient.fetchTagline(), pdfReceiptGenerator::render)
                .thenCompose(bytes -> receiptStorage.upload(key, bytes, "application/pdf"))
                .orTimeout(timeoutSeconds, TimeUnit.SECONDS);
    }
}
