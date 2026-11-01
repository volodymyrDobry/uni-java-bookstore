package com.cengage.contenttaggingservice.unibookstore.service.impl;

import com.cengage.contenttaggingservice.unibookstore.domain.model.Book;
import com.cengage.contenttaggingservice.unibookstore.domain.model.BookStock;
import com.cengage.contenttaggingservice.unibookstore.messaging.ReservationStatus;
import com.cengage.contenttaggingservice.unibookstore.messaging.ReserveStockCommand;
import com.cengage.contenttaggingservice.unibookstore.messaging.ReserveStockLine;
import com.cengage.contenttaggingservice.unibookstore.messaging.StockReservationResult;
import com.cengage.contenttaggingservice.unibookstore.repository.BookStockRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockReservationServiceImplTest {

    @Mock
    private BookStockRepository bookStockRepository;

    @InjectMocks
    private StockReservationServiceImpl service;

    private BookStock stock(long bookId, String title, String author, int quantity, String price) {
        Book book = Book.builder().id(bookId).title(title).author(author).build();
        return BookStock.builder().id(bookId).quantity(quantity).price(new BigDecimal(price)).book(book).build();
    }

    private ReserveStockCommand command(ReserveStockLine... lines) {
        return new ReserveStockCommand(5L, "alice", Instant.now(), List.of(lines));
    }

    @Test
    void reserveDecrementsEveryLineAndReturnsPricedReservedResult() {
        BookStock first = stock(1L, "Dune", "Herbert", 5, "9.99");
        BookStock second = stock(2L, "1984", "Orwell", 4, "4.50");
        when(bookStockRepository.findByBookId(1L)).thenReturn(first);
        when(bookStockRepository.findByBookId(2L)).thenReturn(second);

        StockReservationResult result = service.reserve(
                command(new ReserveStockLine(1L, 2), new ReserveStockLine(2L, 1)));

        assertThat(result.status()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(first.getQuantity()).as("5 - 2").isEqualTo(3);
        assertThat(second.getQuantity()).as("4 - 1").isEqualTo(3);
        assertThat(result.orderId()).isEqualTo(5L);
        assertThat(result.lines())
                .extracting("bookId", "title", "author", "quantity", "unitPrice")
                .containsExactly(
                        tuple(1L, "Dune", "Herbert", 2, new BigDecimal("9.99")),
                        tuple(2L, "1984", "Orwell", 1, new BigDecimal("4.50")));
        verify(bookStockRepository).saveAll(any());
    }

    @Test
    void reserveRejectsWhenOneLineIsInsufficientAndDecrementsNothing() {
        BookStock plenty = stock(1L, "Dune", "Herbert", 5, "9.99");
        BookStock scarce = stock(2L, "1984", "Orwell", 1, "4.50");
        when(bookStockRepository.findByBookId(1L)).thenReturn(plenty);
        when(bookStockRepository.findByBookId(2L)).thenReturn(scarce);

        StockReservationResult result = service.reserve(
                command(new ReserveStockLine(1L, 2), new ReserveStockLine(2L, 3)));

        assertThat(result.status()).isEqualTo(ReservationStatus.INSUFFICIENT_STOCK);
        assertThat(result.lines()).isEmpty();
        assertThat(result.shortfalls())
                .singleElement()
                .extracting("bookId", "requested", "available")
                .containsExactly(2L, 3, 1);
        assertThat(plenty.getQuantity()).as("nothing removed when the whole order cannot be fulfilled").isEqualTo(5);
        assertThat(scarce.getQuantity()).isEqualTo(1);
        verify(bookStockRepository, never()).saveAll(any());
    }

    @Test
    void reserveRejectsWhenStockRowIsMissing() {
        when(bookStockRepository.findByBookId(1L)).thenReturn(null);

        StockReservationResult result = service.reserve(command(new ReserveStockLine(1L, 1)));

        assertThat(result.status()).isEqualTo(ReservationStatus.INSUFFICIENT_STOCK);
        assertThat(result.shortfalls())
                .singleElement()
                .extracting("bookId", "requested", "available")
                .containsExactly(1L, 1, 0);
        verify(bookStockRepository, never()).saveAll(any());
    }

    @Test
    void reserveAllowsExactlyAvailableQuantity() {
        BookStock exact = stock(1L, "Dune", "Herbert", 2, "9.99");
        when(bookStockRepository.findByBookId(1L)).thenReturn(exact);

        StockReservationResult result = service.reserve(command(new ReserveStockLine(1L, 2)));

        assertThat(result.status()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(exact.getQuantity()).isZero();
    }
}
