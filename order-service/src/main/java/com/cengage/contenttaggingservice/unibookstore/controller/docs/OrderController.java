package com.cengage.contenttaggingservice.unibookstore.controller.docs;

import com.cengage.contenttaggingservice.unibookstore.dto.OrderResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;

@RequestMapping("/api/v1/orders")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Orders", description = "Authenticated user's orders and receipts")
public interface OrderController {

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Place an order", description = "Creates an order from the current user's basket. Stock is decremented synchronously; the PDF receipt is produced asynchronously, so the order is returned in PENDING state.")
    OrderResponse placeOrder();

    @GetMapping
    @Operation(summary = "List my orders")
    List<OrderResponse> getMyOrders();

    @GetMapping("/{id}/receipt")
    @Operation(summary = "Download my order receipt", description = "Returns the stored PDF once the order is PDF_READY.")
    ResponseEntity<Resource> downloadReceipt(@PathVariable Long id);
}
