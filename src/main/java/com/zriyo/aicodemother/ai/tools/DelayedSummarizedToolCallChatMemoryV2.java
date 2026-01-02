package com.zriyo.aicodemother.ai.tools;

import com.zriyo.aicodemother.util.RedisUtils;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class DelayedSummarizedToolCallChatMemoryV2 implements ChatMemory {

    private final ChatMemory delegate;
    private final String key;
    private static final String PROJECT_ROOT = System.getProperty("user.dir") + "/generated-project/";

    public DelayedSummarizedToolCallChatMemoryV2(ChatMemory delegate, String key) {
        this.delegate = delegate;
        this.key = key;
    }

    @Override
    public void add(ChatMessage message) {
        if (message instanceof AiMessage aiMsg) {
            delegate.add(aiMsg);
        } else if (message instanceof ToolExecutionResultMessage toolResultMsg) {
            delegate.add(toolResultMsg);
            try {
                // 2. 从 Redis 获取依赖文件路径列表
                List<String> filePathList = RedisUtils.getCacheList(key);
                if (filePathList != null && !filePathList.isEmpty()) {
                    StringBuilder contextBuilder = new StringBuilder();
                    contextBuilder.append("[Reference Files for Context]\n");

                    boolean hasValidFile = false;
                    for (String relativePath : filePathList) {
                        if (relativePath == null || relativePath.trim().isEmpty()) continue;

                        try {
                            String fullPath = PROJECT_ROOT + relativePath;
                            String content = Files.readString(Paths.get(fullPath));
                            contextBuilder.append("\n--- File: ").append(relativePath).append(" ---\n")
                                    .append(content)
                                    .append("\n");
                            hasValidFile = true;
                        } catch (IOException e) {
                            log.warn("Failed to read dependency file: {}", relativePath, e);
                        }
                    }

                    // 3. 如果有有效文件内容，作为用户消息加入上下文
                    if (hasValidFile) {
                        UserMessage referenceContext = UserMessage.from(contextBuilder.toString());
                        delegate.add(referenceContext);
                        log.debug("Added {} dependency files as user context", filePathList.size());
                    }
                }
            } catch (Exception e) {
                log.error("Error loading dependency context from Redis or disk", e);
            }

        } else {
            // 用户消息、系统消息等，直接透传
            delegate.add(message);
        }
    }

    // ===== 以下方法保持不变 =====

    @Override
    public List<ChatMessage> messages() {
        // 注意：toolResultBuffer 实际未使用，可考虑移除
        return new ArrayList<>(delegate.messages());
    }

    @Override
    public Object id() {
        return delegate.id();
    }

    @Override
    public void clear() {
        delegate.clear();
    }
}
