package com.cengage.contenttaggingservice.unibookstore.service.impl;

import com.cengage.contenttaggingservice.unibookstore.domain.model.BookStock;
import com.cengage.contenttaggingservice.unibookstore.dto.BookResponse;
import com.cengage.contenttaggingservice.unibookstore.dto.BookStockResponse;
import com.cengage.contenttaggingservice.unibookstore.dto.StockRequest;
import com.cengage.contenttaggingservice.unibookstore.repository.BookStockRepository;
import com.cengage.contenttaggingservice.unibookstore.service.BookStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BookStockServiceImpl implements BookStockService {

    private final BookStockRepository repository;

    @Override
    public BookStockResponse updateBookStock(Long bookId, StockRequest request) {
        BookStock stock = repository.findByBookId(bookId);
        stock.setPrice(request.price());
        stock.setQuantity(request.quantity());
        repository.save(stock);
        return new BookStockResponse(bookId, request.price(), request.quantity());
    }
}
