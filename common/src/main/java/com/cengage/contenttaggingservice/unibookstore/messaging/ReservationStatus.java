package com.cengage.contenttaggingservice.unibookstore.messaging;

/** Outcome of a stock reservation, carried by {@link StockReservationResult}. */
public enum ReservationStatus {

    /** Stock was removed for every line. */
    RESERVED,

    /** At least one line lacked sufficient stock; nothing was removed. */
    INSUFFICIENT_STOCK,

    /** The reservation could not be completed because of an unexpected error. */
    ERROR
}
