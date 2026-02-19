package com.zriyo.aicodemother;

import dev.langchain4j.community.store.embedding.redis.spring.RedisEmbeddingStoreAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Zriyo AI Code Mother 主启动类
 *
 * @author Zriyo AI
 * @since 2025-02-17
 */
@EnableRetry
@EnableScheduling
@SpringBootApplication(
        scanBasePackages = {
                "com.zriyo",
                "com.anji"
        },
        exclude = {RedisEmbeddingStoreAutoConfiguration.class}
)
@MapperScan("com.zriyo")
public class ZriyoAiCodeMotherApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZriyoAiCodeMotherApplication.class, args);
    }

}
