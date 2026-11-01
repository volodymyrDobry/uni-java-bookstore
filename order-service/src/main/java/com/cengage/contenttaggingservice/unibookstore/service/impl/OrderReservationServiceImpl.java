package com.cengage.contenttaggingservice.unibookstore.service.impl;

import com.cengage.contenttaggingservice.unibookstore.domain.enums.OrderStatus;
import com.cengage.contenttaggingservice.unibookstore.domain.model.Ordering;
import com.cengage.contenttaggingservice.unibookstore.dto.OrderLine;
import com.cengage.contenttaggingservice.unibookstore.dto.OrderSnapshot;
import com.cengage.contenttaggingservice.unibookstore.messaging.ReservedLine;
import com.cengage.contenttaggingservice.unibookstore.messaging.StockReservationResult;
import com.cengage.contenttaggingservice.unibookstore.repository.OrderingRepository;
import com.cengage.contenttaggingservice.unibookstore.service.AdminOrderNotifier;
import com.cengage.contenttaggingservice.unibookstore.service.OrderReservationService;
import com.cengage.contenttaggingservice.unibookstore.service.ReceiptPipeline;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderReservationServiceImpl implements OrderReservationService {

    private final OrderingRepository orderingRepository;
    private final AdminOrderNotifier adminOrderNotifier;
    private final ReceiptPipeline receiptPipeline;

    @Override
    @Transactional
    public void applyResult(StockReservationResult result) {
        Ordering order = orderingRepository.findById(result.orderId()).orElse(null);
        if (order == null) {
            log.warn("Reservation result for unknown order {}; ignoring", result.orderId());
            return;
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            // A redelivered reply, or one that races the receipt pipeline: already handled.
            log.info("Order {} is {}, not PENDING; skipping duplicate reservation result",
                    result.orderId(), order.getStatus());
            return;
        }

        switch (result.status()) {
            case RESERVED -> confirm(order, result);
            case INSUFFICIENT_STOCK -> {
                order.setStatus(OrderStatus.REJECTED);
                log.warn("Order {} rejected: {}", result.orderId(), result.message());
            }
            case ERROR -> {
                order.setStatus(OrderStatus.FAILED);
                log.error("Order {} failed during reservation: {}", result.orderId(), result.message());
            }
            default -> log.warn("Order {} received unknown reservation status {}",
                    result.orderId(), result.status());
        }
    }

    private void confirm(Ordering order, StockReservationResult result) {
        // Persist the prices locally so receipts (and recovery) never need stock data.
        Map<Long, BigDecimal> priceByBook = result.lines().stream()
                .collect(Collectors.toMap(ReservedLine::bookId, ReservedLine::unitPrice));
        order.getItems().forEach(item -> {
            BigDecimal price = priceByBook.get(item.getBook().getId());
            if (price != null) {
                item.setUnitPrice(price);
            }
        });

        OrderSnapshot snapshot = toSnapshot(result);
        runAfterCommit(() -> {
            adminOrderNotifier.notifyNewOrder(snapshot);
            receiptPipeline.generateAndStore(snapshot);
        });
        log.info("Stock reserved for order {}; receipt pipeline scheduled", result.orderId());
    }

    private OrderSnapshot toSnapshot(StockReservationResult result) {
        List<OrderLine> lines = new ArrayList<>();
        for (ReservedLine reserved : result.lines()) {
            lines.add(new OrderLine(reserved.bookId(), reserved.title(), reserved.author(),
                    reserved.quantity(), reserved.unitPrice()));
        }
        BigDecimal grandTotal = lines.stream()
                .map(line -> line.unitPrice().multiply(BigDecimal.valueOf(line.count())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new OrderSnapshot(result.orderId(), result.customerId(), result.createdAt(),
                List.copyOf(lines), grandTotal);
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
