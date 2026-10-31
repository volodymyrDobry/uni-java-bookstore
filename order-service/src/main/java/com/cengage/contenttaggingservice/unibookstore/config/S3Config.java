package com.cengage.contenttaggingservice.unibookstore.config;

import com.cengage.contenttaggingservice.unibookstore.properties.S3Properties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

import java.net.URI;
import java.util.concurrent.CompletionException;

@Slf4j
@Configuration
@EnableConfigurationProperties(S3Properties.class)
public class S3Config {

    @Bean
    public S3AsyncClient s3AsyncClient(S3Properties properties) {
        return S3AsyncClient.builder()
                .endpointOverride(URI.create(properties.getEndpoint()))
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey())))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }

    @Bean
    public ApplicationRunner ensureReceiptsBucket(S3AsyncClient s3AsyncClient, S3Properties properties) {
        return args -> {
            try {
                s3AsyncClient.createBucket(CreateBucketRequest.builder()
                        .bucket(properties.getBucket())
                        .build()).join();
                log.info("Created S3 bucket '{}'", properties.getBucket());
            } catch (CompletionException ex) {
                log.info("S3 bucket '{}' already present or unavailable: {}",
                        properties.getBucket(), ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage());
            }
        };
    }
}
