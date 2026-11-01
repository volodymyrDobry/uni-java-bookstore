package com.cengage.contenttaggingservice.unibookstore.service;

import com.cengage.contenttaggingservice.unibookstore.dto.BookStockResponse;
import com.cengage.contenttaggingservice.unibookstore.dto.StockRequest;

public interface BookStockService {
    BookStockResponse updateBookStock(Long bookId, StockRequest request);
}
