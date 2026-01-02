package com.zriyo.aicodemother.ai.model.config;

import com.zriyo.aicodemother.ai.key.RedisApiKeyScheduler;
import com.zriyo.aicodemother.ai.model.proxy.AiModelExecutor;
import com.zriyo.aicodemother.ai.model.proxy.ChatModelProxy;
import com.zriyo.aicodemother.ai.service.AiModelMonitorListener;
import com.zriyo.aicodemother.model.enums.AiCodeGenStage;
import dev.langchain4j.model.chat.ChatModel;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "langchain4j.open-ai.chat-model")
@Data
@EqualsAndHashCode(callSuper = true)
public class ChatModelConfig extends BaseAiConfig {

    private Boolean strictJsonSchema = true;

    @Resource
    private RedisApiKeyScheduler apiKeyScheduler;

    @Resource
    private AiModelExecutor aiModelExecutor;

    @PostConstruct
    public void registerKeys() {
        apiKeyScheduler.setStageModelMap(AiCodeGenStage.SKELETON, getModelName());
        if (getApiKeys() != null && !getApiKeys().isEmpty()) {
            apiKeyScheduler.registerModelKeys(AiCodeGenStage.SKELETON, getApiKeys());
        }
    }

    @Bean
    @Primary
    public ChatModel chatModel(AiModelMonitorListener aiModelMonitorListener) {

        if (getModelName() == null || getModelName().isBlank()) {
            throw new IllegalStateException("'model-name' is missing in application.yml under langchain4j.open-ai.chat-model");
        }
        return new ChatModelProxy(aiModelExecutor, this, AiCodeGenStage.SKELETON, List.of(aiModelMonitorListener));
    }
}
