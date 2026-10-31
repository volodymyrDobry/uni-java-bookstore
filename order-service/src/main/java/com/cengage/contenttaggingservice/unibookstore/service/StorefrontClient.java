package com.cengage.contenttaggingservice.unibookstore.service;

import com.cengage.contenttaggingservice.unibookstore.config.AsyncConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
public class StorefrontClient {

    @Async(AsyncConfig.ORDERING_EXECUTOR)
    public CompletableFuture<String> fetchTagline() {
        log.info("Fetching storefront tagline on {}", Thread.currentThread().getName());
        return CompletableFuture.completedFuture("Thank you for shopping at Uni Book Store!");
    }
}
