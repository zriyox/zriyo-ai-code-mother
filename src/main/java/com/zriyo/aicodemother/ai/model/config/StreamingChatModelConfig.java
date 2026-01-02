package com.zriyo.aicodemother.ai.model.config;

import com.zriyo.aicodemother.ai.key.RedisApiKeyScheduler;
import com.zriyo.aicodemother.ai.model.proxy.AiModelExecutor;
import com.zriyo.aicodemother.ai.model.proxy.StreamingChatModelProxy;
import com.zriyo.aicodemother.ai.service.AiModelMonitorListener;
import com.zriyo.aicodemother.model.enums.AiCodeGenStage;
import dev.langchain4j.model.chat.StreamingChatModel;
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
@ConfigurationProperties(prefix = "langchain4j.open-ai.streaming-chat-model")
@Data
@EqualsAndHashCode(callSuper = true)
public class StreamingChatModelConfig extends BaseAiConfig {

    private Double topP;

    @Resource
    private AiModelMonitorListener aiModelMonitorListener;
    @Resource
    private RedisApiKeyScheduler apiKeyScheduler;
    @Resource
    private AiModelExecutor aiModelExecutor;

    @PostConstruct
    public void registerKeys() {
        // 1. 建立阶段与模型的映射关系
        apiKeyScheduler.setStageModelMap(AiCodeGenStage.CODE_GENERATION, getModelName());
        // 2. 将配置文件中的 apiKeys 注册到 Redis 池中
        if (getApiKeys() != null && !getApiKeys().isEmpty()) {
            apiKeyScheduler.registerModelKeys(AiCodeGenStage.CODE_GENERATION, getApiKeys());
        }
    }

    /**
     * 核心改变：返回带代理逻辑的 StreamingChatModelProxy
     */
    @Bean
    @Primary
    public StreamingChatModel streamingChatModel() {
        return new StreamingChatModelProxy(
                apiKeyScheduler,
                this,
                AiCodeGenStage.CODE_GENERATION,
                List.of(aiModelMonitorListener)
        );
    }
}
