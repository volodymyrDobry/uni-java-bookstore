package com.cengage.contenttaggingservice.unibookstore.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes the single stock-reservation result back to order-service.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockEventPublisher {

    private final JmsTemplate jmsTemplate;

    public void publishResult(StockReservationResult result) {
        log.info("Publishing reservation result for order {}: {}", result.orderId(), result.status());
        jmsTemplate.convertAndSend(JmsQueues.STOCK_RESERVE_RESULT, result);
    }
}
