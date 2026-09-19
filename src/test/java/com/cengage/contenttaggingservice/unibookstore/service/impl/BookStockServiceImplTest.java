package com.cengage.contenttaggingservice.unibookstore.service.impl;

import com.cengage.contenttaggingservice.unibookstore.domain.model.BookStock;
import com.cengage.contenttaggingservice.unibookstore.dto.BookStockResponse;
import com.cengage.contenttaggingservice.unibookstore.dto.StockRequest;
import com.cengage.contenttaggingservice.unibookstore.repository.BookStockRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookStockServiceImplTest {

    private static final Long BOOK_ID = 42L;
    private static final Long STOCK_ID = 7L;

    @Mock
    private BookStockRepository repository;

    @InjectMocks
    private BookStockServiceImpl bookStockService;

    @Test
    void whenUpdateBookStockThenStockValuesReplacedAndPersisted() {
        // Given
        BookStock existingStock = BookStock.builder()
                .id(STOCK_ID)
                .quantity(1)
                .price(BigDecimal.ONE)
                .build();
        StockRequest request = new StockRequest(5, new BigDecimal("9.99"));

        when(repository.findByBookId(BOOK_ID))
                .thenReturn(existingStock);
        when(repository.save(any(BookStock.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        BookStockResponse actualResponse = bookStockService.updateBookStock(BOOK_ID, request);

        // Then
        BookStockResponse expectedResponse =
                new BookStockResponse(BOOK_ID, new BigDecimal("9.99"), 5);

        ArgumentCaptor<BookStock> stockCaptor = ArgumentCaptor.forClass(BookStock.class);
        verify(repository, times(1)).save(stockCaptor.capture());
        BookStock savedStock = stockCaptor.getValue();

        assertThat(savedStock.getId())
                .as("the existing stock row must be updated, not replaced")
                .isEqualTo(STOCK_ID);
        assertThat(savedStock.getQuantity()).isEqualTo(5);
        assertThat(savedStock.getPrice()).isEqualByComparingTo("9.99");

        assertThat(actualResponse).usingRecursiveComparison().isEqualTo(expectedResponse);
    }
}
