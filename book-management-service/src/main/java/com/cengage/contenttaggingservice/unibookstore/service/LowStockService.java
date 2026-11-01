package com.cengage.contenttaggingservice.unibookstore.service;

import com.cengage.contenttaggingservice.unibookstore.dto.LowStockItem;

import java.util.List;

/**
 * Computes low-stock reports from the stock data owned by this service.
 */
public interface LowStockService {

    List<LowStockItem> findLowStock(int threshold, boolean onlyEnabled);
}
