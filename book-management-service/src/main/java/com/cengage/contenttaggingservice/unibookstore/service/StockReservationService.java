package com.cengage.contenttaggingservice.unibookstore.service;

import com.cengage.contenttaggingservice.unibookstore.messaging.ReserveStockCommand;
import com.cengage.contenttaggingservice.unibookstore.messaging.StockReservationResult;

/**
 * Removes stock for a placed order. book-management-service owns the stock, so all
 * decrements happen here. The returned {@link StockReservationResult} is the single
 * event published back to order-service.
 */
public interface StockReservationService {

    StockReservationResult reserve(ReserveStockCommand command);
}
