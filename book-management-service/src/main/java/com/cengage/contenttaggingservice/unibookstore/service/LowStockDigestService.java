package com.cengage.contenttaggingservice.unibookstore.service;

import com.cengage.contenttaggingservice.unibookstore.dto.LowStockDigestResponse;

import java.util.List;

/**
 * Captures low-stock reports to the database and reads them back for the admin UI.
 */
public interface LowStockDigestService {

    /** Computes a low-stock report for the given parameters, stores it, and returns it. */
    LowStockDigestResponse recordDigest(int threshold, boolean onlyEnabled, String channel);

    /** Returns the most recently recorded digests, newest first. */
    List<LowStockDigestResponse> recentDigests(int limit);
}
