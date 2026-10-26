package com.cengage.contenttaggingservice.unibookstore.service.impl;

import com.cengage.contenttaggingservice.unibookstore.properties.S3Properties;
import com.cengage.contenttaggingservice.unibookstore.service.ReceiptStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.BytesWrapper;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class S3ReceiptStorage implements ReceiptStorage {

    private final S3AsyncClient s3AsyncClient;
    private final S3Properties properties;

    @Override
    public CompletableFuture<String> upload(String key, byte[] content, String contentType) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(key)
                .contentType(contentType)
                .build();
        return s3AsyncClient.putObject(request, AsyncRequestBody.fromBytes(content))
                .thenApply(response -> key);
    }

    @Override
    public CompletableFuture<byte[]> download(String key) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(key)
                .build();
        return s3AsyncClient.getObject(request, AsyncResponseTransformer.toBytes())
                .thenApply(BytesWrapper::asByteArray);
    }
}
