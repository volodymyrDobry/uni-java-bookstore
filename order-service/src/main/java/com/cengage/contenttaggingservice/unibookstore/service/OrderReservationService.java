package com.cengage.contenttaggingservice.unibookstore.service;

import com.cengage.contenttaggingservice.unibookstore.messaging.StockReservationResult;

/**
 * Applies the single stock-reservation result to the local order: reserved moves it
 * towards a receipt, insufficient stock marks it REJECTED, and an error marks it FAILED.
 */
public interface OrderReservationService {

    void applyResult(StockReservationResult result);
}
