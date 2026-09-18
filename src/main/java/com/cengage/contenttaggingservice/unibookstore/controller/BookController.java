package com.cengage.contenttaggingservice.unibookstore.controller;

import com.cengage.contenttaggingservice.unibookstore.domain.enums.Genre;
import com.cengage.contenttaggingservice.unibookstore.controller.docs.BookManagementController;
import com.cengage.contenttaggingservice.unibookstore.dto.BookRequest;
import com.cengage.contenttaggingservice.unibookstore.dto.BookResponse;
import com.cengage.contenttaggingservice.unibookstore.service.BookService;

import java.math.BigDecimal;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class BookController implements BookManagementController {
    private final BookService service;

    @Override
    public BookResponse create(BookRequest request) {
        return service.create(request);
    }

    @Override
    public BookResponse update(Long id, BookRequest request) {
        return service.update(id, request);
    }

    @Override
    public BookResponse setVisibility(Long id, boolean enabled) {
        return service.setEnabled(id, enabled);
    }

    @Override
    public Page<BookResponse> available(String query, Genre genre,
                                        BigDecimal minPrice, BigDecimal maxPrice,
                                        Pageable pageable) {
        return service.available(query, genre, minPrice, maxPrice, pageable);
    }

    @Override
    public Page<BookResponse> findAll(Pageable pageable) {
        return service.findAll(pageable);
    }
}
