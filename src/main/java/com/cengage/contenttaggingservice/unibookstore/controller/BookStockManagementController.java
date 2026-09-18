package com.cengage.contenttaggingservice.unibookstore.controller;

import com.cengage.contenttaggingservice.unibookstore.dto.BookResponse;
import com.cengage.contenttaggingservice.unibookstore.controller.docs.BookStockController;
import com.cengage.contenttaggingservice.unibookstore.dto.QuantityRequest;
import com.cengage.contenttaggingservice.unibookstore.dto.StockRequest;
import com.cengage.contenttaggingservice.unibookstore.service.BookService;

import java.math.BigDecimal;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class BookStockManagementController implements BookStockController {
    private final BookService service;

    @Override
    public BookResponse set(Long bookId, StockRequest request) {
        return service.createOrUpdateStock(bookId, request);
    }

    @Override
    public BookResponse add(Long bookId, QuantityRequest request) {
        return service.addStock(bookId, request.quantity());
    }

    @Override
    public BookResponse remove(Long bookId, QuantityRequest request) {
        return service.removeStock(bookId, request.quantity());
    }

    @Override
    public BookResponse price(Long bookId, BigDecimal price) {
        return service.updatePrice(bookId, price);
    }
}
