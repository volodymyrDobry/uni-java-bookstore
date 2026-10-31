package com.cengage.contenttaggingservice.unibookstore.controller;

import com.cengage.contenttaggingservice.unibookstore.dto.BookStockResponse;
import com.cengage.contenttaggingservice.unibookstore.dto.StockRequest;
import com.cengage.contenttaggingservice.unibookstore.service.BookStockService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookStockControllerImplTest {

    @Mock
    private BookStockService bookStockService;

    @InjectMocks
    private BookStockControllerImpl controller;

    @Test
    void updateBookStockDelegatesToService() {
        StockRequest request = new StockRequest(5, new BigDecimal("9.99"));
        BookStockResponse expected = new BookStockResponse(42L, new BigDecimal("9.99"), 5);
        when(bookStockService.updateBookStock(42L, request)).thenReturn(expected);

        assertThat(controller.updateBookStock(42L, request)).isSameAs(expected);
    }
}
