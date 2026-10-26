package com.cengage.contenttaggingservice.unibookstore.service;

import java.util.concurrent.CompletableFuture;

public interface ReceiptStorage {

    CompletableFuture<String> upload(String key, byte[] content, String contentType);

    CompletableFuture<byte[]> download(String key);
}
