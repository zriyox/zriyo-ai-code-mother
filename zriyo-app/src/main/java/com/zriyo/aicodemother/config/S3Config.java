package com.zriyo.aicodemother.config;

import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
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

/**
 * S3/OOS 对象存储配置
 *
 * @author Zriyo AI
 * @since 2025-02-17
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "rustfs.s3")
public class S3Config {

    /**
     * S3/OOS 端点
     * 示例: https://io.zriyo.com
     * 注意: 不要带 bucket
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
    @ConditionalOnExpression("'${rustfs.s3.endpoint:}' != '' and '${rustfs.s3.endpoint:}' != null")
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
                                .pathStyleAccessEnabled(true) // 必须启用 Path-Style
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
     * 预签名 URL 专用 Presigner
     * 必须配置 Path-Style 才能正确生成 bucket.domain 格式的 URL
     */
    @Bean
    @ConditionalOnExpression("'${rustfs.s3.endpoint:}' != '' and '${rustfs.s3.endpoint:}' != null")
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
                                .pathStyleAccessEnabled(true) // 关键配置
                                .build()
                )
                .build();
    }

    /**
     * Bucket 存储桶配置
     */
    @Data
    public static class BucketSpec {

        /**
         * 真实 bucket 名
         * 例如: zriyo-user
         */
        private String name;

        private BucketType type = BucketType.PRIVATE;

        public enum BucketType {
            /**
             * 公共读 bucket
             */
            PUBLIC,
            /**
             * 私有 bucket (需要预签名 URL)
             */
            PRIVATE
        }
    }
}
