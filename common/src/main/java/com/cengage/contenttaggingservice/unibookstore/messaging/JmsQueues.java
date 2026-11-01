package com.cengage.contenttaggingservice.unibookstore.messaging;

/**
 * Queue names for the order-placement saga. order-service sends the command and
 * consumes the single result; book-management-service consumes the command and
 * replies once.
 */
public final class JmsQueues {

    /** order-service -> book-management-service: reserve and remove stock for an order. */
    public static final String STOCK_RESERVE_REQUEST = "stock.reserve.request";

    /** book-management-service -> order-service: the single outcome of the reservation. */
    public static final String STOCK_RESERVE_RESULT = "stock.reserve.result";

    private JmsQueues() {
    }
}
