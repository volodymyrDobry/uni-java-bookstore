package com.cengage.contenttaggingservice.unibookstore.controller;

import com.cengage.contenttaggingservice.unibookstore.controller.docs.LowStockDigestController;
import com.cengage.contenttaggingservice.unibookstore.dto.LowStockDigestResponse;
import com.cengage.contenttaggingservice.unibookstore.service.LowStockDigestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class LowStockDigestControllerImpl implements LowStockDigestController {

    private static final String MANUAL_CHANNEL = "MANUAL";

    private final LowStockDigestService lowStockDigestService;

    @Override
    public List<LowStockDigestResponse> recentDigests(int limit) {
        return lowStockDigestService.recentDigests(limit);
    }

    @Override
    public LowStockDigestResponse generateDigest(int threshold, boolean onlyEnabled) {
        return lowStockDigestService.recordDigest(threshold, onlyEnabled, MANUAL_CHANNEL);
    }
}
