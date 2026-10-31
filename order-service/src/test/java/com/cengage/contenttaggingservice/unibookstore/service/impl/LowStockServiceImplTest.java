package com.cengage.contenttaggingservice.unibookstore.service.impl;

import com.cengage.contenttaggingservice.unibookstore.domain.model.Book;
import com.cengage.contenttaggingservice.unibookstore.domain.model.BookStock;
import com.cengage.contenttaggingservice.unibookstore.dto.LowStockItem;
import com.cengage.contenttaggingservice.unibookstore.repository.BookStockRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LowStockServiceImplTest {

    @Mock
    private BookStockRepository bookStockRepository;

    @InjectMocks
    private LowStockServiceImpl service;

    private BookStock stock(long bookId, String title, boolean enabled, int quantity) {
        Book book = Book.builder().id(bookId).title(title).enabled(enabled).build();
        return BookStock.builder().id(bookId).quantity(quantity).price(new BigDecimal("9.99")).book(book).build();
    }

    @Test
    void onlyEnabledFiltersOutDisabledBooks() {
        when(bookStockRepository.findLowStock(5)).thenReturn(List.of(
                stock(1L, "Enabled", true, 2),
                stock(2L, "Disabled", false, 1)));

        List<LowStockItem> items = service.reportLowStock(5, true, "LOG");

        assertThat(items).extracting(LowStockItem::bookId).containsExactly(1L);
    }

    @Test
    void includesDisabledBooksWhenOnlyEnabledIsFalse() {
        when(bookStockRepository.findLowStock(5)).thenReturn(List.of(
                stock(1L, "Enabled", true, 2),
                stock(2L, "Disabled", false, 1)));

        List<LowStockItem> items = service.reportLowStock(5, false, "LOG");

        assertThat(items).extracting(LowStockItem::bookId).containsExactly(1L, 2L);
    }

    @Test
    void passesThresholdThroughToTheRepository() {
        when(bookStockRepository.findLowStock(25)).thenReturn(List.of());

        service.reportLowStock(25, true, "SSE");

        verify(bookStockRepository).findLowStock(25);
    }
}
