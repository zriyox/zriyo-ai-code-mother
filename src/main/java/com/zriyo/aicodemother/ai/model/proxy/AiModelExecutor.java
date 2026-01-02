package com.zriyo.aicodemother.ai.model.proxy;

import com.zriyo.aicodemother.ai.key.RedisApiKeyScheduler;
import com.zriyo.aicodemother.model.enums.AiCodeGenStage;
import jakarta.annotation.Resource;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@Getter
public class AiModelExecutor {
    @Resource
    private RedisApiKeyScheduler apiKeyScheduler;

    /**
     * 通用的执行模板
     * @param stage 阶段
     * @param maxRetries 最大换 Key 次数
     * @param action 执行的具体动作 (传入选中的 Key，返回是否成功)
     */
    public void executeWithRetry(AiCodeGenStage stage, int maxRetries, java.util.function.Predicate<String> action) {
        int attempts = 0;
        while (attempts < maxRetries) {
            String apiKey = apiKeyScheduler.acquire(stage);
            try {
                // 执行动作，如果返回 true 表示成功
                if (action.test(apiKey)) {
                    return;
                }
                // 如果返回 false，也上报失败并继续重试
                apiKeyScheduler.reportFailure(apiKey, stage);
            } catch (Exception e) {
                log.error("[AI-Executor] Key {} 失败: {}", apiKey.substring(0,8), e.getMessage());
                apiKeyScheduler.reportFailure(apiKey, stage);
            } finally {
                apiKeyScheduler.release(stage, apiKey); // 统一归还并发额度
            }
            attempts++;
            backoff(attempts);
        }
        throw new RuntimeException("AI 执行耗尽重试次数");
    }

    private void backoff(int attempt) {
        try { Thread.sleep(Math.min(500L * attempt, 3000L)); } catch (InterruptedException ignored) {}
    }
}
