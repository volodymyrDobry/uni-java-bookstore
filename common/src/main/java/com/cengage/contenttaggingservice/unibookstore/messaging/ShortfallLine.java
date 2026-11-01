package com.cengage.contenttaggingservice.unibookstore.messaging;

/** Describes a line that could not be reserved: how much was requested vs available. */
public record ShortfallLine(
        Long bookId,
        int requested,
        int available
) {
}
