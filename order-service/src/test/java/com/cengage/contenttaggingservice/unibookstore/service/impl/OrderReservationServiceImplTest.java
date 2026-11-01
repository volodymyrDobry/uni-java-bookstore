package com.cengage.contenttaggingservice.unibookstore.service.impl;

import com.cengage.contenttaggingservice.unibookstore.domain.enums.OrderStatus;
import com.cengage.contenttaggingservice.unibookstore.domain.model.Book;
import com.cengage.contenttaggingservice.unibookstore.domain.model.OrderItem;
import com.cengage.contenttaggingservice.unibookstore.domain.model.Ordering;
import com.cengage.contenttaggingservice.unibookstore.dto.OrderSnapshot;
import com.cengage.contenttaggingservice.unibookstore.messaging.ReservedLine;
import com.cengage.contenttaggingservice.unibookstore.messaging.ShortfallLine;
import com.cengage.contenttaggingservice.unibookstore.messaging.StockReservationResult;
import com.cengage.contenttaggingservice.unibookstore.repository.OrderingRepository;
import com.cengage.contenttaggingservice.unibookstore.service.AdminOrderNotifier;
import com.cengage.contenttaggingservice.unibookstore.service.ReceiptPipeline;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderReservationServiceImplTest {

    @Mock
    private OrderingRepository orderingRepository;

    @Mock
    private AdminOrderNotifier adminOrderNotifier;

    @Mock
    private ReceiptPipeline receiptPipeline;

    @InjectMocks
    private OrderReservationServiceImpl service;

    private Ordering pendingOrder() {
        Book book = Book.builder().id(1L).title("Dune").author("Herbert").build();
        Ordering order = Ordering.builder().id(5L).customerId("alice").createdAt(Instant.now())
                .status(OrderStatus.PENDING).build();
        order.addItem(OrderItem.builder().book(book).count(2).build());
        return order;
    }

    private StockReservationResult reserved() {
        return StockReservationResult.reserved(5L, "alice", Instant.now(),
                List.of(new ReservedLine(1L, "Dune", "Herbert", 2, new BigDecimal("9.99"))));
    }

    @Test
    void reservedResultPersistsPricesNotifiesAdminAndSchedulesReceipt() {
        Ordering order = pendingOrder();
        when(orderingRepository.findById(5L)).thenReturn(Optional.of(order));

        service.applyResult(reserved());

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING); // receipt pipeline flips it to PDF_READY
        assertThat(order.getItems().get(0).getUnitPrice()).isEqualByComparingTo("9.99");

        ArgumentCaptor<OrderSnapshot> captor = ArgumentCaptor.forClass(OrderSnapshot.class);
        verify(adminOrderNotifier).notifyNewOrder(captor.capture());
        verify(receiptPipeline).generateAndStore(any(OrderSnapshot.class));
        assertThat(captor.getValue().grandTotal()).isEqualByComparingTo("19.98");
        assertThat(captor.getValue().orderId()).isEqualTo(5L);
    }

    @Test
    void insufficientStockResultMovesOrderToRejected() {
        Ordering order = pendingOrder();
        when(orderingRepository.findById(5L)).thenReturn(Optional.of(order));

        service.applyResult(StockReservationResult.insufficientStock(5L, "alice", Instant.now(),
                List.of(new ShortfallLine(1L, 2, 0))));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.REJECTED);
        verifyNoInteractions(adminOrderNotifier, receiptPipeline);
    }

    @Test
    void errorResultMovesOrderToFailed() {
        Ordering order = pendingOrder();
        when(orderingRepository.findById(5L)).thenReturn(Optional.of(order));

        service.applyResult(StockReservationResult.error(5L, "alice", Instant.now(), "db down"));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.FAILED);
        verifyNoInteractions(adminOrderNotifier, receiptPipeline);
    }

    @Test
    void resultIsIdempotentWhenOrderIsNoLongerPending() {
        Ordering order = pendingOrder();
        order.setStatus(OrderStatus.PDF_READY);
        when(orderingRepository.findById(5L)).thenReturn(Optional.of(order));

        service.applyResult(reserved());

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PDF_READY);
        assertThat(order.getItems().get(0).getUnitPrice()).isNull();
        verifyNoInteractions(adminOrderNotifier, receiptPipeline);
    }

    @Test
    void resultForUnknownOrderIsIgnored() {
        when(orderingRepository.findById(5L)).thenReturn(Optional.empty());

        service.applyResult(reserved());

        verifyNoInteractions(adminOrderNotifier, receiptPipeline);
    }
}
