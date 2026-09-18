package com.cengage.contenttaggingservice.unibookstore.controller.docs;

import com.cengage.contenttaggingservice.unibookstore.dto.BookResponse;
import com.cengage.contenttaggingservice.unibookstore.dto.QuantityRequest;
import com.cengage.contenttaggingservice.unibookstore.dto.StockRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/v1/books/{bookId}/stock")
@Tag(name = "Book stock", description = "Administrator stock and pricing operations")
public interface BookStockController {
    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create or replace stock")
    BookResponse set(@PathVariable Long bookId, @Valid @RequestBody StockRequest request);

    @PostMapping("/add")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Add stock")
    BookResponse add(@PathVariable Long bookId, @Valid @RequestBody QuantityRequest request);

    @PostMapping("/remove")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Remove stock")
    BookResponse remove(@PathVariable Long bookId, @Valid @RequestBody QuantityRequest request);

    @PatchMapping("/price")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update price")
    BookResponse price(@PathVariable Long bookId, @RequestParam @NotNull @Positive BigDecimal price);
}
