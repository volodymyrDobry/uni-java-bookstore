package com.cengage.contenttaggingservice.unibookstore.service;

import com.cengage.contenttaggingservice.unibookstore.domain.enums.Genre;
import com.cengage.contenttaggingservice.unibookstore.domain.model.Book;
import com.cengage.contenttaggingservice.unibookstore.dto.BookRequest;
import com.cengage.contenttaggingservice.unibookstore.dto.BookResponse;
import com.cengage.contenttaggingservice.unibookstore.dto.StockRequest;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BookService {
    BookResponse create(BookRequest request);

    BookResponse update(Long id, BookRequest request);

    BookResponse setEnabled(Long id, boolean enabled);

    Page<BookResponse> available(String query, Genre genre, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    Page<BookResponse> findAll(Pageable pageable);

    BookResponse createOrUpdateStock(Long bookId, StockRequest request);

    BookResponse addStock(Long bookId, int quantity);

    BookResponse removeStock(Long bookId, int quantity);

    BookResponse updatePrice(Long bookId, BigDecimal price);

    Book book(Long id);

    void requireAvailable(Long bookId, int requestedQuantity);
}
