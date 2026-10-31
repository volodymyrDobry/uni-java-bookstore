package com.cengage.contenttaggingservice.unibookstore.service;

import com.cengage.contenttaggingservice.unibookstore.config.AsyncConfig;
import com.cengage.contenttaggingservice.unibookstore.dto.OrderSnapshot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
public class ReceiptAssembler {

    @Async(AsyncConfig.ORDERING_EXECUTOR)
    public CompletableFuture<OrderSnapshot> buildModel(OrderSnapshot snapshot) {
        log.info("Assembling invoice model for order {} on {}", snapshot.orderId(), Thread.currentThread().getName());
        return CompletableFuture.completedFuture(snapshot);
    }
}
