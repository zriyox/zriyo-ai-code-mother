package com.zriyo.aicodemother.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.retry.backoff.BackoffStrategy;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

@Data
@Configuration
@ConfigurationProperties(prefix = "rustfs.s3")
public class S3Config {

    /**
     * 示例:
     * https://io.zriyo.com
     * ❗ 不要带 bucket
     */
    private String endpoint;

    private String accessKey;
    private String secretKey;

    private Map<String, BucketSpec> buckets;

    /**
     * 普通 S3 Client
     * - list / put / delete / head 等
     * - 强制 Path-Style
     */
    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.US_EAST_1)
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(accessKey, secretKey)
                        )
                )
                .serviceConfiguration(
                        S3Configuration.builder()
                                .pathStyleAccessEnabled(true) // ⭐ 必须
                                .build()
                )
                .httpClient(
                        ApacheHttpClient.builder()
                                .maxConnections(50)
                                .connectionTimeout(Duration.ofSeconds(5))
                                .socketTimeout(Duration.ofSeconds(30))
                                .build()
                )
                .overrideConfiguration(
                        ClientOverrideConfiguration.builder()
                                .apiCallTimeout(Duration.ofSeconds(30))
                                .apiCallAttemptTimeout(Duration.ofSeconds(10))
                                .retryPolicy(
                                        RetryPolicy.builder()
                                                .numRetries(3)
                                                .backoffStrategy(
                                                        BackoffStrategy.defaultThrottlingStrategy()
                                                )
                                                .build()
                                )
                                .build()
                )
                .build();
    }

    /**
     * ⭐ 预签名 URL 专用
     * 如果这里不配 Path-Style
     * 你永远都会拿到 bucket.domain
     */
    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.US_EAST_1)
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(accessKey, secretKey)
                        )
                )
                .serviceConfiguration(
                        S3Configuration.builder()
                                .pathStyleAccessEnabled(true) // ⭐ 关键中的关键
                                .build()
                )
                .build();
    }

    @Data
    public static class BucketSpec {

        /**
         * 真实 bucket 名
         * 例如: zriyo-user
         */
        private String name;

        private BucketType type = BucketType.PRIVATE;

        public enum BucketType {
            PUBLIC,
            PRIVATE
        }
    }
}
