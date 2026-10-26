package com.cengage.contenttaggingservice.unibookstore.service.impl;

import com.cengage.contenttaggingservice.unibookstore.domain.enums.OrderStatus;
import com.cengage.contenttaggingservice.unibookstore.domain.model.Basket;
import com.cengage.contenttaggingservice.unibookstore.domain.model.Book;
import com.cengage.contenttaggingservice.unibookstore.domain.model.BookItem;
import com.cengage.contenttaggingservice.unibookstore.domain.model.BookStock;
import com.cengage.contenttaggingservice.unibookstore.domain.model.Ordering;
import com.cengage.contenttaggingservice.unibookstore.dto.OrderResponse;
import com.cengage.contenttaggingservice.unibookstore.dto.OrderSnapshot;
import com.cengage.contenttaggingservice.unibookstore.dto.ReceiptDownload;
import com.cengage.contenttaggingservice.unibookstore.exception.InsufficientStockException;
import com.cengage.contenttaggingservice.unibookstore.exception.NotFoundException;
import com.cengage.contenttaggingservice.unibookstore.exception.UserNotAllowedException;
import com.cengage.contenttaggingservice.unibookstore.mapper.OrderMapper;
import com.cengage.contenttaggingservice.unibookstore.repository.BasketRepository;
import com.cengage.contenttaggingservice.unibookstore.repository.BookStockRepository;
import com.cengage.contenttaggingservice.unibookstore.repository.OrderingRepository;
import com.cengage.contenttaggingservice.unibookstore.security.CurrentUser;
import com.cengage.contenttaggingservice.unibookstore.service.AdminOrderNotifier;
import com.cengage.contenttaggingservice.unibookstore.service.ReceiptPipeline;
import com.cengage.contenttaggingservice.unibookstore.service.ReceiptStorage;
import com.cengage.contenttaggingservice.unibookstore.utils.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderingServiceImplTest {

    @Mock
    private BasketRepository basketRepository;

    @Mock
    private BookStockRepository bookStockRepository;

    @Mock
    private OrderingRepository orderingRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private ReceiptPipeline receiptPipeline;

    @Mock
    private AdminOrderNotifier adminOrderNotifier;

    @Mock
    private ReceiptStorage receiptStorage;

    private OrderingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OrderingServiceImpl(
                basketRepository, bookStockRepository, orderingRepository, orderMapper,
                securityUtils, receiptPipeline, adminOrderNotifier, receiptStorage, 10L);
    }

    private void loginAs(String username) {
        when(securityUtils.getCurrentUser()).thenReturn(new CurrentUser(username, List.of("USER")));
    }

    private Basket basketWith(BookItem... items) {
        return Basket.builder().id(1L).userId("alice").books(new HashSet<>(Set.of(items))).build();
    }

    private BookItem bookItem(long bookId, int quantity) {
        Book book = Book.builder().id(bookId).title("Dune").author("Herbert").build();
        return BookItem.builder().id(bookId).quantity(quantity).book(book).build();
    }

    @Test
    void placeOrderPersistsOrderDecrementsStockClearsBasketAndTriggersAsync() {
        loginAs("alice");
        Basket basket = basketWith(bookItem(1L, 2));
        when(basketRepository.findByUserId("alice")).thenReturn(Optional.of(basket));
        BookStock stock = BookStock.builder().id(1L).quantity(5).price(new BigDecimal("9.99")).build();
        when(bookStockRepository.findByBookId(1L)).thenReturn(stock);
        when(orderingRepository.save(any(Ordering.class))).thenAnswer(invocation -> {
            Ordering order = invocation.getArgument(0);
            order.setId(100L);
            return order;
        });
        when(orderMapper.toResponse(any(Ordering.class)))
                .thenReturn(new OrderResponse(100L, "alice", Instant.now(), OrderStatus.PENDING, null, List.of()));

        OrderResponse response = service.placeOrder();

        assertThat(response.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(stock.getQuantity()).as("stock decremented 5 - 2").isEqualTo(3);
        assertThat(basket.getBooks()).as("basket cleared").isEmpty();

        ArgumentCaptor<Ordering> orderCaptor = ArgumentCaptor.forClass(Ordering.class);
        verify(orderingRepository).save(orderCaptor.capture());
        Ordering saved = orderCaptor.getValue();
        assertThat(saved.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(saved.getItems()).hasSize(1);
        assertThat(saved.getItems().get(0).getCount()).isEqualTo(2);

        ArgumentCaptor<OrderSnapshot> snapshotCaptor = ArgumentCaptor.forClass(OrderSnapshot.class);
        verify(receiptPipeline).generateAndStore(snapshotCaptor.capture());
        assertThat(snapshotCaptor.getValue().grandTotal()).isEqualByComparingTo("19.98");
        verify(adminOrderNotifier).notifyNewOrder(any(OrderSnapshot.class));
    }

    @Test
    void placeOrderRejectsWhenStockInsufficientAndPersistsNothing() {
        loginAs("alice");
        when(basketRepository.findByUserId("alice")).thenReturn(Optional.of(basketWith(bookItem(1L, 5))));
        when(bookStockRepository.findByBookId(1L))
                .thenReturn(BookStock.builder().id(1L).quantity(1).price(new BigDecimal("9.99")).build());

        assertThatThrownBy(service::placeOrder).isInstanceOf(InsufficientStockException.class);

        verify(orderingRepository, never()).save(any());
        verify(receiptPipeline, never()).generateAndStore(any());
        verify(adminOrderNotifier, never()).notifyNewOrder(any());
    }

    @Test
    void placeOrderRejectsEmptyBasket() {
        loginAs("alice");
        when(basketRepository.findByUserId("alice")).thenReturn(Optional.of(basketWith()));

        assertThatThrownBy(service::placeOrder).isInstanceOf(InsufficientStockException.class);
        verify(orderingRepository, never()).save(any());
    }

    @Test
    void getMyOrdersMapsResults() {
        loginAs("alice");
        Ordering order = Ordering.builder().id(5L).customerId("alice").createdAt(Instant.now())
                .status(OrderStatus.PDF_READY).build();
        when(orderingRepository.findByCustomerIdOrderByCreatedAtDesc("alice")).thenReturn(List.of(order));
        OrderResponse mapped = new OrderResponse(5L, "alice", order.getCreatedAt(), OrderStatus.PDF_READY, "k", List.of());
        when(orderMapper.toResponse(order)).thenReturn(mapped);

        assertThat(service.getMyOrders()).containsExactly(mapped);
    }

    @Test
    void downloadReceiptReturnsBytesForOwnerWhenReady() {
        loginAs("alice");
        Ordering order = Ordering.builder().id(5L).customerId("alice").createdAt(Instant.now())
                .status(OrderStatus.PDF_READY).pdfLocation("receipts/alice/5.pdf").build();
        when(orderingRepository.findById(5L)).thenReturn(Optional.of(order));
        when(receiptStorage.download("receipts/alice/5.pdf"))
                .thenReturn(CompletableFuture.completedFuture(new byte[]{9, 9}));

        ReceiptDownload download = service.downloadReceipt(5L);

        assertThat(download.filename()).isEqualTo("order-5.pdf");
        assertThat(download.content()).containsExactly(9, 9);
    }

    @Test
    void downloadReceiptRejectsNonOwner() {
        loginAs("alice");
        Ordering order = Ordering.builder().id(5L).customerId("bob").createdAt(Instant.now())
                .status(OrderStatus.PDF_READY).pdfLocation("k").build();
        when(orderingRepository.findById(5L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.downloadReceipt(5L)).isInstanceOf(UserNotAllowedException.class);
    }

    @Test
    void downloadReceiptFailsWhenNotReady() {
        loginAs("alice");
        Ordering order = Ordering.builder().id(5L).customerId("alice").createdAt(Instant.now())
                .status(OrderStatus.PENDING).build();
        when(orderingRepository.findById(5L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.downloadReceipt(5L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void downloadReceiptFailsWhenOrderMissing() {
        loginAs("alice");
        when(orderingRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.downloadReceipt(5L)).isInstanceOf(NotFoundException.class);
    }
}
