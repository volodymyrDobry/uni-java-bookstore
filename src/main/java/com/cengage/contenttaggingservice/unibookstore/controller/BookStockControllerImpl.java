package com.cengage.contenttaggingservice.unibookstore.controller;

import com.cengage.contenttaggingservice.unibookstore.controller.docs.BookStockController;
import com.cengage.contenttaggingservice.unibookstore.dto.BookStockResponse;
import com.cengage.contenttaggingservice.unibookstore.dto.StockRequest;
import com.cengage.contenttaggingservice.unibookstore.service.BookStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class BookStockControllerImpl implements BookStockController {

    private final BookStockService bookStockService;

    @Override
    public BookStockResponse updateBookStock(Long bookId, StockRequest request) {
        return bookStockService.updateBookStock(bookId, request);
    }
}
