package com.cengage.contenttaggingservice.unibookstore.service.impl;

import com.cengage.contenttaggingservice.unibookstore.domain.enums.OrderStatus;
import com.cengage.contenttaggingservice.unibookstore.domain.model.Basket;
import com.cengage.contenttaggingservice.unibookstore.domain.model.Book;
import com.cengage.contenttaggingservice.unibookstore.domain.model.BookItem;
import com.cengage.contenttaggingservice.unibookstore.domain.model.BookStock;
import com.cengage.contenttaggingservice.unibookstore.domain.model.OrderItem;
import com.cengage.contenttaggingservice.unibookstore.domain.model.Ordering;
import com.cengage.contenttaggingservice.unibookstore.dto.OrderLine;
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
import com.cengage.contenttaggingservice.unibookstore.service.AdminOrderNotifier;
import com.cengage.contenttaggingservice.unibookstore.service.OrderingService;
import com.cengage.contenttaggingservice.unibookstore.service.ReceiptPipeline;
import com.cengage.contenttaggingservice.unibookstore.service.ReceiptStorage;
import com.cengage.contenttaggingservice.unibookstore.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
public class OrderingServiceImpl implements OrderingService {

    private final BasketRepository basketRepository;
    private final BookStockRepository bookStockRepository;
    private final OrderingRepository orderingRepository;
    private final OrderMapper orderMapper;
    private final SecurityUtils securityUtils;
    private final ReceiptPipeline receiptPipeline;
    private final AdminOrderNotifier adminOrderNotifier;
    private final ReceiptStorage receiptStorage;
    private final long downloadTimeoutSeconds;

    public OrderingServiceImpl(BasketRepository basketRepository,
                               BookStockRepository bookStockRepository,
                               OrderingRepository orderingRepository,
                               OrderMapper orderMapper,
                               SecurityUtils securityUtils,
                               ReceiptPipeline receiptPipeline,
                               AdminOrderNotifier adminOrderNotifier,
                               ReceiptStorage receiptStorage,
                               @Value("${app.async.download-timeout-seconds:10}") long downloadTimeoutSeconds) {
        this.basketRepository = basketRepository;
        this.bookStockRepository = bookStockRepository;
        this.orderingRepository = orderingRepository;
        this.orderMapper = orderMapper;
        this.securityUtils = securityUtils;
        this.receiptPipeline = receiptPipeline;
        this.adminOrderNotifier = adminOrderNotifier;
        this.receiptStorage = receiptStorage;
        this.downloadTimeoutSeconds = downloadTimeoutSeconds;
    }

    @Override
    @Transactional
    public OrderResponse placeOrder() {
        String customerId = securityUtils.getCurrentUser().username();
        Basket basket = basketRepository.findByUserId(customerId)
                .orElseThrow(() -> new InsufficientStockException("Cannot place an order: basket is empty"));

        Set<BookItem> basketItems = basket.getBooks();
        if (basketItems.isEmpty()) {
            throw new InsufficientStockException("Cannot place an order: basket is empty");
        }

        Ordering order = Ordering.builder()
                .customerId(customerId)
                .createdAt(Instant.now())
                .status(OrderStatus.PENDING)
                .build();

        List<OrderLine> lines = new ArrayList<>();
        for (BookItem basketItem : basketItems) {
            Book book = basketItem.getBook();
            int count = basketItem.getQuantity();
            BookStock stock = bookStockRepository.findByBookId(book.getId());
            if (stock == null || stock.getQuantity() < count) {
                throw new InsufficientStockException("Not enough stock for book " + book.getId());
            }
            stock.setQuantity(stock.getQuantity() - count);

            order.addItem(OrderItem.builder().book(book).count(count).build());
            lines.add(new OrderLine(book.getId(), book.getTitle(), book.getAuthor(), count, stock.getPrice()));
        }

        Ordering saved = orderingRepository.save(order);
        basket.getBooks().clear();
        basketRepository.save(basket);

        BigDecimal grandTotal = lines.stream()
                .map(line -> line.unitPrice().multiply(BigDecimal.valueOf(line.count())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        OrderSnapshot snapshot = new OrderSnapshot(
                saved.getId(), customerId, saved.getCreatedAt(), List.copyOf(lines), grandTotal);

        runAfterCommit(() -> {
            adminOrderNotifier.notifyNewOrder(snapshot);
            receiptPipeline.generateAndStore(snapshot);
        });

        return orderMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders() {
        String customerId = securityUtils.getCurrentUser().username();
        return orderingRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ReceiptDownload downloadReceipt(Long orderId) {
        String customerId = securityUtils.getCurrentUser().username();
        Ordering order = orderingRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));
        if (!Objects.equals(order.getCustomerId(), customerId)) {
            throw new UserNotAllowedException("Current user is not allowed to download this receipt");
        }
        if (order.getStatus() != OrderStatus.PDF_READY || order.getPdfLocation() == null) {
            throw new NotFoundException("Receipt is not ready yet");
        }

        byte[] content = awaitReceipt(order.getPdfLocation());
        return new ReceiptDownload("order-" + orderId + ".pdf", content);
    }

    private byte[] awaitReceipt(String pdfLocation) {
        try {
            return receiptStorage.download(pdfLocation).get(downloadTimeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while downloading receipt", ex);
        } catch (ExecutionException | TimeoutException ex) {
            throw new IllegalStateException("Failed to download receipt", ex);
        }
    }

    private void runAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}
