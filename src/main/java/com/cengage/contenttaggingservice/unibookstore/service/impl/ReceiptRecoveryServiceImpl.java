package com.cengage.contenttaggingservice.unibookstore.service.impl;

import com.cengage.contenttaggingservice.unibookstore.domain.enums.OrderStatus;
import com.cengage.contenttaggingservice.unibookstore.domain.model.Book;
import com.cengage.contenttaggingservice.unibookstore.domain.model.BookStock;
import com.cengage.contenttaggingservice.unibookstore.domain.model.OrderItem;
import com.cengage.contenttaggingservice.unibookstore.domain.model.Ordering;
import com.cengage.contenttaggingservice.unibookstore.dto.OrderLine;
import com.cengage.contenttaggingservice.unibookstore.dto.OrderSnapshot;
import com.cengage.contenttaggingservice.unibookstore.repository.BookStockRepository;
import com.cengage.contenttaggingservice.unibookstore.repository.OrderingRepository;
import com.cengage.contenttaggingservice.unibookstore.service.ReceiptPipeline;
import com.cengage.contenttaggingservice.unibookstore.service.ReceiptRecoveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiptRecoveryServiceImpl implements ReceiptRecoveryService {

    private final OrderingRepository orderingRepository;
    private final BookStockRepository bookStockRepository;
    private final ReceiptPipeline receiptPipeline;
    private final ObjectProvider<ReceiptRecoveryService> selfProvider;

    @Override
    public void recoverPendingReceipts(int maxAgeMinutes, int batchSize, int maxAttempts) {
        ReceiptRecoveryService self = selfProvider.getObject();
        List<Long> orderIds = findRecoverableOrderIds(maxAgeMinutes, batchSize, maxAttempts);
        log.info("ReceiptRecovery picked {} order(s) (maxAgeMinutes={}, maxAttempts={})",
                orderIds.size(), maxAgeMinutes, maxAttempts);
        for (Long orderId : orderIds) {
            try {
                self.recoverOne(orderId, maxAttempts);
            } catch (RuntimeException ex) {
                log.error("Unexpected error recovering order {}; continuing with the rest", orderId, ex);
            }
        }
    }

    @Override
    public List<Long> findRecoverableOrderIds(int maxAgeMinutes, int batchSize, int maxAttempts) {
        Instant cutoff = Instant.now().minus(maxAgeMinutes, ChronoUnit.MINUTES);
        return orderingRepository.findRecoverableIds(
                OrderStatus.PDF_READY, cutoff, maxAttempts, PageRequest.of(0, batchSize));
    }

    @Override
    @Transactional
    public void recoverOne(long orderId, int maxAttempts) {
        Ordering order = orderingRepository.findById(orderId).orElse(null);
        if (order == null || order.getStatus() == OrderStatus.PDF_READY) {
            return;
        }

        order.setReceiptAttempts(order.getReceiptAttempts() + 1);
        OrderSnapshot snapshot = buildSnapshot(order);

        try {
            String key = receiptPipeline.generateAndStoreSync(snapshot);
            order.setPdfLocation(key);
            order.setStatus(OrderStatus.PDF_READY);
            log.info("Recovered receipt for order {} at {} (attempt {})", orderId, key, order.getReceiptAttempts());
        } catch (RuntimeException ex) {
            if (order.getReceiptAttempts() >= maxAttempts) {
                order.setStatus(OrderStatus.FAILED);
                log.error("Order {} exhausted {} receipt attempts; marking FAILED", orderId, maxAttempts, ex);
            } else {
                order.setStatus(OrderStatus.PENDING);
                log.warn("Receipt recovery attempt {} failed for order {}: {}",
                        order.getReceiptAttempts(), orderId, ex.getMessage());
            }
        }
    }

    private OrderSnapshot buildSnapshot(Ordering order) {
        List<OrderLine> lines = new ArrayList<>();
        for (OrderItem item : order.getItems()) {
            Book book = item.getBook();
            BookStock stock = bookStockRepository.findByBookId(book.getId());
            BigDecimal unitPrice = stock != null ? stock.getPrice() : BigDecimal.ZERO;
            lines.add(new OrderLine(book.getId(), book.getTitle(), book.getAuthor(), item.getCount(), unitPrice));
        }
        BigDecimal grandTotal = lines.stream()
                .map(line -> line.unitPrice().multiply(BigDecimal.valueOf(line.count())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new OrderSnapshot(order.getId(), order.getCustomerId(), order.getCreatedAt(),
                List.copyOf(lines), grandTotal);
    }
}
