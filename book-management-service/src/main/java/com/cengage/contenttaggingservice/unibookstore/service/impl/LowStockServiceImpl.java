package com.cengage.contenttaggingservice.unibookstore.service.impl;

import com.cengage.contenttaggingservice.unibookstore.dto.LowStockItem;
import com.cengage.contenttaggingservice.unibookstore.repository.BookStockRepository;
import com.cengage.contenttaggingservice.unibookstore.service.LowStockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LowStockServiceImpl implements LowStockService {

    private final BookStockRepository bookStockRepository;

    @Override
    @Transactional(readOnly = true)
    public List<LowStockItem> findLowStock(int threshold, boolean onlyEnabled) {
        List<LowStockItem> items = bookStockRepository.findLowStock(threshold).stream()
                .filter(stock -> !onlyEnabled || stock.getBook().isEnabled())
                .map(stock -> new LowStockItem(stock.getBook().getId(), stock.getBook().getTitle(), stock.getQuantity()))
                .toList();

        log.info("Low-stock query [threshold={}, onlyEnabled={}]: {} book(s) at or below threshold",
                threshold, onlyEnabled, items.size());
        return items;
    }
}
