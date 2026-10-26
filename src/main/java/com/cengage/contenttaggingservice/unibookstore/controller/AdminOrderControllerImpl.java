package com.cengage.contenttaggingservice.unibookstore.controller;

import com.cengage.contenttaggingservice.unibookstore.controller.docs.AdminOrderController;
import com.cengage.contenttaggingservice.unibookstore.service.AdminOrderNotifier;
import com.cengage.contenttaggingservice.unibookstore.service.ReceiptExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;

@RestController
@RequiredArgsConstructor
public class AdminOrderControllerImpl implements AdminOrderController {

    private final AdminOrderNotifier adminOrderNotifier;
    private final ReceiptExportService receiptExportService;

    @Override
    public SseEmitter streamOrders() {
        return adminOrderNotifier.subscribe();
    }

    @Override
    public ResponseEntity<Resource> exportReceipts(Instant from, Instant to) {
        byte[] zip = receiptExportService.exportReceipts(from, to);
        String filename = "receipts-" + from.toEpochMilli() + "-" + to.toEpochMilli() + ".zip";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(new ByteArrayResource(zip));
    }
}
