package com.cengage.contenttaggingservice.unibookstore.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes order-service's stock-reservation command for a freshly placed order.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderMessagePublisher {

    private final JmsTemplate jmsTemplate;

    public void requestStockReservation(ReserveStockCommand command) {
        log.info("Requesting stock reservation for order {}", command.orderId());
        jmsTemplate.convertAndSend(JmsQueues.STOCK_RESERVE_REQUEST, command);
    }
}
