package com.cengage.contenttaggingservice.unibookstore.service.impl;

import com.cengage.contenttaggingservice.unibookstore.domain.enums.OrderStatus;
import com.cengage.contenttaggingservice.unibookstore.domain.model.Book;
import com.cengage.contenttaggingservice.unibookstore.domain.model.BookStock;
import com.cengage.contenttaggingservice.unibookstore.domain.model.OrderItem;
import com.cengage.contenttaggingservice.unibookstore.domain.model.Ordering;
import com.cengage.contenttaggingservice.unibookstore.repository.BookStockRepository;
import com.cengage.contenttaggingservice.unibookstore.repository.OrderingRepository;
import com.cengage.contenttaggingservice.unibookstore.service.ReceiptPipeline;
import com.cengage.contenttaggingservice.unibookstore.service.ReceiptRecoveryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReceiptRecoveryServiceImplTest {

    @Mock
    private OrderingRepository orderingRepository;

    @Mock
    private BookStockRepository bookStockRepository;

    @Mock
    private ReceiptPipeline receiptPipeline;

    @Mock
    private ObjectProvider<ReceiptRecoveryService> selfProvider;

    @InjectMocks
    private ReceiptRecoveryServiceImpl service;

    private Ordering pendingOrderWithItem(int attempts) {
        Book book = Book.builder().id(1L).title("Dune").author("Herbert").build();
        Ordering order = Ordering.builder().id(5L).customerId("alice").createdAt(Instant.now())
                .status(OrderStatus.PENDING).receiptAttempts(attempts).build();
        order.addItem(OrderItem.builder().book(book).count(2).build());
        return order;
    }

    @Test
    void findRecoverableOrderIdsQueriesByCutoffAndAttempts() {
        when(orderingRepository.findRecoverableIds(eq(OrderStatus.PDF_READY), any(Instant.class), eq(3), any()))
                .thenReturn(List.of(1L, 2L));

        assertThat(service.findRecoverableOrderIds(15, 50, 3)).containsExactly(1L, 2L);
    }

    @Test
    void recoverPendingReceiptsDelegatesOneRecoverPerPickedOrder() {
        when(orderingRepository.findRecoverableIds(eq(OrderStatus.PDF_READY), any(Instant.class), eq(3), any()))
                .thenReturn(List.of(1L, 2L));
        ReceiptRecoveryService self = org.mockito.Mockito.mock(ReceiptRecoveryService.class);
        when(selfProvider.getObject()).thenReturn(self);

        service.recoverPendingReceipts(15, 50, 3);

        verify(self).recoverOne(1L, 3);
        verify(self).recoverOne(2L, 3);
    }

    @Test
    void recoverOneIsIdempotentForAlreadyReadyOrder() {
        Ordering ready = Ordering.builder().id(5L).customerId("alice").createdAt(Instant.now())
                .status(OrderStatus.PDF_READY).pdfLocation("receipts/alice/5.pdf").receiptAttempts(1).build();
        when(orderingRepository.findById(5L)).thenReturn(Optional.of(ready));

        service.recoverOne(5L, 3);

        assertThat(ready.getStatus()).isEqualTo(OrderStatus.PDF_READY);
        assertThat(ready.getReceiptAttempts()).isEqualTo(1);
        verifyNoInteractions(receiptPipeline);
    }

    @Test
    void recoverOneSkipsMissingOrder() {
        when(orderingRepository.findById(5L)).thenReturn(Optional.empty());

        service.recoverOne(5L, 3);

        verifyNoInteractions(receiptPipeline);
    }

    @Test
    void recoverOneSuccessMarksReadyAndStoresLocation() {
        Ordering order = pendingOrderWithItem(0);
        when(orderingRepository.findById(5L)).thenReturn(Optional.of(order));
        when(bookStockRepository.findByBookId(1L))
                .thenReturn(BookStock.builder().id(1L).quantity(3).price(new BigDecimal("9.99")).build());
        when(receiptPipeline.generateAndStoreSync(any())).thenReturn("receipts/alice/5.pdf");

        service.recoverOne(5L, 3);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PDF_READY);
        assertThat(order.getPdfLocation()).isEqualTo("receipts/alice/5.pdf");
        assertThat(order.getReceiptAttempts()).isEqualTo(1);
    }

    @Test
    void recoverOneFailureBelowLimitStaysPendingForRetry() {
        Ordering order = pendingOrderWithItem(0);
        when(orderingRepository.findById(5L)).thenReturn(Optional.of(order));
        when(bookStockRepository.findByBookId(1L))
                .thenReturn(BookStock.builder().id(1L).quantity(3).price(new BigDecimal("9.99")).build());
        when(receiptPipeline.generateAndStoreSync(any())).thenThrow(new RuntimeException("s3 down"));

        service.recoverOne(5L, 3);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getReceiptAttempts()).isEqualTo(1);
    }

    @Test
    void recoverOneFailureAtLimitMarksFailed() {
        Ordering order = pendingOrderWithItem(2);
        when(orderingRepository.findById(5L)).thenReturn(Optional.of(order));
        when(bookStockRepository.findByBookId(1L))
                .thenReturn(BookStock.builder().id(1L).quantity(3).price(new BigDecimal("9.99")).build());
        when(receiptPipeline.generateAndStoreSync(any())).thenThrow(new RuntimeException("s3 down"));

        service.recoverOne(5L, 3);

        assertThat(order.getReceiptAttempts()).isEqualTo(3);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.FAILED);
    }

    @Test
    void recoverOneNeverSavesExplicitlyRelyingOnDirtyChecking() {
        when(orderingRepository.findById(5L)).thenReturn(Optional.empty());

        service.recoverOne(5L, 3);

        verify(orderingRepository, never()).save(any());
        verify(orderingRepository).findById(anyLong());
    }
}
