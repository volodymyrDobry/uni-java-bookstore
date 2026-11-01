package com.cengage.contenttaggingservice.unibookstore.messaging;

import com.cengage.contenttaggingservice.unibookstore.service.StockReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

/**
 * Consumes order-service's reserve request, removes the stock, and publishes the
 * single outcome back. The stock removal commits inside the service; an unexpected
 * failure rolls it back and is reported as an ERROR result rather than lost.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockReservationListener {

    private final StockReservationService stockReservationService;
    private final StockEventPublisher stockEventPublisher;

    @JmsListener(destination = JmsQueues.STOCK_RESERVE_REQUEST)
    public void onReserveRequest(ReserveStockCommand command) {
        StockReservationResult result;
        try {
            result = stockReservationService.reserve(command);
        } catch (RuntimeException ex) {
            log.error("Reservation failed for order {}: {}", command.orderId(), ex.getMessage(), ex);
            result = StockReservationResult.error(
                    command.orderId(), command.customerId(), command.createdAt(), ex.getMessage());
        }
        stockEventPublisher.publishResult(result);
    }
}
