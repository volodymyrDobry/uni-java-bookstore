package com.cengage.contenttaggingservice.unibookstore.controller;

import com.cengage.contenttaggingservice.unibookstore.controller.docs.OrderController;
import com.cengage.contenttaggingservice.unibookstore.dto.OrderResponse;
import com.cengage.contenttaggingservice.unibookstore.dto.ReceiptDownload;
import com.cengage.contenttaggingservice.unibookstore.service.OrderingService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class OrderControllerImpl implements OrderController {

    private final OrderingService orderingService;

    @Override
    public OrderResponse placeOrder() {
        return orderingService.placeOrder();
    }

    @Override
    public List<OrderResponse> getMyOrders() {
        return orderingService.getMyOrders();
    }

    @Override
    public ResponseEntity<Resource> downloadReceipt(Long id) {
        ReceiptDownload receipt = orderingService.downloadReceipt(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + receipt.filename() + "\"")
                .body(new ByteArrayResource(receipt.content()));
    }
}
