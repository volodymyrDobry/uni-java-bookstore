package com.cengage.contenttaggingservice.unibookstore.controller.docs;

import com.cengage.contenttaggingservice.unibookstore.annotation.BookExists;
import com.cengage.contenttaggingservice.unibookstore.dto.BookStockResponse;
import com.cengage.contenttaggingservice.unibookstore.dto.StockRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/api/v1/books/{bookId}/stock")
@Validated
@Tag(name = "Book stock", description = "Administrator stock and pricing operations")
public interface BookStockController {

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Replace stock quantity and price")
    BookStockResponse updateBookStock(@BookExists @PathVariable Long bookId, @Valid @RequestBody StockRequest request);
}
