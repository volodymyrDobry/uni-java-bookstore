package com.cengage.contenttaggingservice.unibookstore.messaging;

import com.cengage.contenttaggingservice.unibookstore.service.OrderReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

/**
 * Receives the single stock-reservation result from book-management-service and
 * applies it to the order. Kept thin: the state change lives in
 * {@link OrderReservationService} so it runs inside a transaction.
 */
@Component
@RequiredArgsConstructor
public class StockReservationReplyListener {

    private final OrderReservationService orderReservationService;

    @JmsListener(destination = JmsQueues.STOCK_RESERVE_RESULT)
    public void onResult(StockReservationResult result) {
        orderReservationService.applyResult(result);
    }
}
