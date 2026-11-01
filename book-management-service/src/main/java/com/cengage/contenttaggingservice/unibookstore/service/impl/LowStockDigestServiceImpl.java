package com.cengage.contenttaggingservice.unibookstore.service.impl;

import com.cengage.contenttaggingservice.unibookstore.domain.model.LowStockDigest;
import com.cengage.contenttaggingservice.unibookstore.domain.model.LowStockDigestItem;
import com.cengage.contenttaggingservice.unibookstore.dto.LowStockDigestResponse;
import com.cengage.contenttaggingservice.unibookstore.dto.LowStockItem;
import com.cengage.contenttaggingservice.unibookstore.mapper.LowStockDigestMapper;
import com.cengage.contenttaggingservice.unibookstore.repository.LowStockDigestRepository;
import com.cengage.contenttaggingservice.unibookstore.service.LowStockDigestService;
import com.cengage.contenttaggingservice.unibookstore.service.LowStockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LowStockDigestServiceImpl implements LowStockDigestService {

    private final LowStockService lowStockService;
    private final LowStockDigestRepository lowStockDigestRepository;
    private final LowStockDigestMapper lowStockDigestMapper;

    @Override
    @Transactional
    public LowStockDigestResponse recordDigest(int threshold, boolean onlyEnabled, String channel) {
        List<LowStockItem> items = lowStockService.findLowStock(threshold, onlyEnabled);

        LowStockDigest digest = LowStockDigest.builder()
                .createdAt(Instant.now())
                .threshold(threshold)
                .onlyEnabled(onlyEnabled)
                .channel(channel)
                .itemCount(items.size())
                .build();
        items.forEach(item -> digest.addItem(LowStockDigestItem.builder()
                .bookId(item.bookId())
                .title(item.title())
                .quantity(item.quantity())
                .build()));

        LowStockDigest saved = lowStockDigestRepository.save(digest);
        log.info("Stored low-stock digest {} [channel={}, threshold={}]: {} book(s)",
                saved.getId(), channel, threshold, saved.getItemCount());
        return lowStockDigestMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LowStockDigestResponse> recentDigests(int limit) {
        return lowStockDigestRepository.findByOrderByCreatedAtDescIdDesc(PageRequest.of(0, limit)).stream()
                .map(lowStockDigestMapper::toResponse)
                .toList();
    }
}
