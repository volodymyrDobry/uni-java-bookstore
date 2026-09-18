package com.cengage.contenttaggingservice.unibookstore.controller.docs;

import com.cengage.contenttaggingservice.unibookstore.dto.CreateBookRequest;
import com.cengage.contenttaggingservice.unibookstore.dto.BookResponse;
import com.cengage.contenttaggingservice.unibookstore.dto.GetBooksRequest;
import com.cengage.contenttaggingservice.unibookstore.dto.UpdateBookDetailsRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

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
    BookResponse createBook(@Valid @RequestBody CreateBookRequest request);

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update book details")
    BookResponse updateBookDetails(@PathVariable Long id, @Valid @RequestBody UpdateBookDetailsRequest request);

    @GetMapping
    @Operation(summary = "Find all books", description = "Searches title/description and filters in-stock enabled books")
    Page<BookResponse> findAllBooks(@ModelAttribute GetBooksRequest getBooksRequest, Pageable pageable);
}
