package com.zriyo.aicodemother;

import dev.langchain4j.community.store.embedding.redis.spring.RedisEmbeddingStoreAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(
        scanBasePackages = {
                "com.zriyo.aicodemother",
                "com.anji"
        },
        exclude = {RedisEmbeddingStoreAutoConfiguration.class}
)
@MapperScan("com.zriyo.aicodemother.mapper")
@EnableRetry
@EnableScheduling
@EnableConfigurationProperties()
public class ZriyoAiCodeMotherApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZriyoAiCodeMotherApplication.class, args);
    }

}
