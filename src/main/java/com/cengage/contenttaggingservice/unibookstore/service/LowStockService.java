package com.cengage.contenttaggingservice.unibookstore.service;

import com.cengage.contenttaggingservice.unibookstore.dto.LowStockItem;

import java.util.List;

public interface LowStockService {

    List<LowStockItem> reportLowStock(int threshold, boolean onlyEnabled, String channel);
}
