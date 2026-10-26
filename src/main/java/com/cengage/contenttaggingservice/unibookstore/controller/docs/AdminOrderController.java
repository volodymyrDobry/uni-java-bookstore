package com.cengage.contenttaggingservice.unibookstore.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;

@RequestMapping("/api/v1/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin orders", description = "Administrator order stream and receipt exports")
public interface AdminOrderController {

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Subscribe to the live new-order stream (SSE)")
    SseEmitter streamOrders();

    @GetMapping("/receipts/export")
    @Operation(summary = "Export receipts in a period as a ZIP", description = "from/to are ISO-8601 instants, e.g. 2026-10-01T00:00:00Z.")
    ResponseEntity<Resource> exportReceipts(@RequestParam Instant from, @RequestParam Instant to);
}
