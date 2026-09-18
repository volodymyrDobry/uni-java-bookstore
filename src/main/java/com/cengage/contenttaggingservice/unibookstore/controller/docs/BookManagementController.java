package com.cengage.contenttaggingservice.unibookstore.controller.docs;

import com.cengage.contenttaggingservice.unibookstore.domain.enums.Genre;
import com.cengage.contenttaggingservice.unibookstore.dto.BookRequest;
import com.cengage.contenttaggingservice.unibookstore.dto.BookResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/v1/books")
@Tag(name = "Books", description = "Book catalogue management and available-book search")
public interface BookManagementController {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a book")
    BookResponse create(@Valid @RequestBody BookRequest request);

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update book details")
    BookResponse update(@PathVariable Long id, @Valid @RequestBody BookRequest request);

    @PatchMapping("/{id}/visibility")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Show or hide a book")
    BookResponse setVisibility(@PathVariable Long id, @RequestParam boolean enabled);

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Find available books", description = "Searches title/description and filters in-stock enabled books")
    Page<BookResponse> available(@RequestParam(required = false) String query, @RequestParam(required = false) Genre genre, @RequestParam(required = false) BigDecimal minPrice, @RequestParam(required = false) BigDecimal maxPrice, Pageable pageable);

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Find all books for administration", description = "Includes hidden and out-of-stock books")
    Page<BookResponse> findAll(Pageable pageable);
}
