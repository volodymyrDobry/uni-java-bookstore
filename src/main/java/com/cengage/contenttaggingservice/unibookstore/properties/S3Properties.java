package com.cengage.contenttaggingservice.unibookstore.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.s3")
public class S3Properties {
    private String endpoint;
    private String region;
    private String bucket;
    private String accessKey;
    private String secretKey;
}
