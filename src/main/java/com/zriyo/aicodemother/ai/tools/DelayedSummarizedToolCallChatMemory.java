package com.zriyo.aicodemother.ai.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zriyo.aicodemother.ai.AiCodeGenTypeRoutingService;
import com.zriyo.aicodemother.model.AppConstant;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.ChatMemory;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Deprecated
public class DelayedSummarizedToolCallChatMemory implements ChatMemory {

    private final AiCodeGenTypeRoutingService aiCodeGenTypeRoutingService;
    private final ChatMemory delegate;
    private AiMessage lastAiMessage = null;

    private final List<ToolExecutionResultMessage> toolResultBuffer = new ArrayList<>();
    private static final int SUMMARY_BATCH_SIZE = 5;
    private static final int SINGLE_MSG_MAX_LENGTH = 500;

    private static final Set<String> NO_TRUNCATE_FILES = Set.of(
            AppConstant.STATIC_ENTRY_FILE,
            "package.json",
            "vite.config.js",
            "src/main.js",
            "src/router/index.js"
    );

    private final ObjectMapper objectMapper = new ObjectMapper();

    public DelayedSummarizedToolCallChatMemory(ChatMemory delegate,
                                               AiCodeGenTypeRoutingService aiCodeGenTypeRoutingService) {
        this.delegate = delegate;
        this.aiCodeGenTypeRoutingService = aiCodeGenTypeRoutingService;
    }

    @Override
    public void add(ChatMessage message) {
        if (message instanceof AiMessage aiMsg) {
            lastAiMessage = aiMsg;

            String filePath = extractFilePathFromAiMessage(aiMsg);
            if (filePath != null && NO_TRUNCATE_FILES.contains(filePath)) {
                delegate.add(message); // 免截断
                return;
            }

            String content = aiMsg.text();
            if (content != null && content.length() > SINGLE_MSG_MAX_LENGTH) {
                content = generateSummary(content);
                message = AiMessage.from(content, aiMsg.toolExecutionRequests());
            }

            delegate.add(message);
        } else if (message instanceof ToolExecutionResultMessage toolMsg) {
            String filePath = extractFilePathFromToolMessage(toolMsg);

            String content = toolMsg.text();
            if (filePath == null || !NO_TRUNCATE_FILES.contains(filePath)) {
                if (content != null && content.length() > SINGLE_MSG_MAX_LENGTH) {
                    content = generateSummary(content);
                    toolMsg = ToolExecutionResultMessage.from(toolMsg.id(), toolMsg.toolName(), content);
                }
            }

            toolResultBuffer.add(toolMsg);

            if (toolResultBuffer.size() >= SUMMARY_BATCH_SIZE) {
                flushToolBuffer();
            }
        } else {
            delegate.add(message);
        }
    }

    private void flushToolBuffer() {
        if (toolResultBuffer.isEmpty()) return;

        List<ToolExecutionResultMessage> batch = new ArrayList<>(toolResultBuffer);
        toolResultBuffer.clear();

        try {
            StringBuilder sb = new StringBuilder();
            for (ToolExecutionResultMessage msg : batch) {
                sb.append("Tool '").append(msg.toolName()).append("' (ID=").append(msg.id()).append("): ")
                        .append(msg.text()).append("\n");
            }

            String prompt = sb.toString().trim() + "\n\n请以简洁 JSON 格式总结上述工具调用结果的关键信息。";

            String llmSummary;
            try {
                llmSummary = aiCodeGenTypeRoutingService.summarizeContext(prompt);
                if (llmSummary == null || llmSummary.isBlank()) {
                    throw new RuntimeException("LLM returned blank");
                }
            } catch (Exception e) {
                log.warn("LLM 摘要失败，回退到本地聚合摘要", e);
                llmSummary = "[LOCAL_TOOL_SUMMARY] " + generateSummary(sb.toString());
            }

            Set<String> idsToRemove = batch.stream().map(ToolExecutionResultMessage::id).collect(Collectors.toSet());
            List<ChatMessage> current = new ArrayList<>(delegate.messages());
            List<ChatMessage> newMessages = new ArrayList<>();
            boolean hasNonToolMessage = false;

            for (ChatMessage msg : current) {
                if (msg instanceof ToolExecutionResultMessage tool &&
                        idsToRemove.contains(tool.id())) {
                    continue;
                }
                newMessages.add(msg);
                if (!(msg instanceof ToolExecutionResultMessage)) hasNonToolMessage = true;
            }

            if (!hasNonToolMessage && !current.isEmpty()) {
                for (int i = current.size() - 1; i >= 0; i--) {
                    ChatMessage msg = current.get(i);
                    if (!(msg instanceof ToolExecutionResultMessage)) {
                        newMessages.add(msg);
                        log.warn("摘要后上下文无非工具消息，保留最后一条非工具消息: {}", msg);
                        break;
                    }
                }
            }

            delegate.clear();
            newMessages.forEach(delegate::add);
            delegate.add(AiMessage.from("[TOOL_CONTEXT_SUMMARY]\n" + llmSummary.trim()));

            log.debug("已用摘要替换 {} 条工具结果消息", batch.size());

        } catch (Exception e) {
            log.error("工具结果摘要替换失败，回退：将原始工具消息加入上下文", e);
            batch.forEach(delegate::add);
        }
    }

    private String generateSummary(String content) {
        if (content == null || content.isEmpty()) return "";
        final int len = SINGLE_MSG_MAX_LENGTH;
        if (content.length() <= len) return content;
        int start = Math.max(0, content.length() - len);
        while (start > 0 && Character.isLowSurrogate(content.charAt(start))) start++;
        return "...[tail truncated]\n" + content.substring(start);
    }

    @Override
    public List<ChatMessage> messages() {
        List<ChatMessage> all = new ArrayList<>(delegate.messages());
        all.addAll(toolResultBuffer);
        return all;
    }

    @Override
    public Object id() {
        return delegate.id();
    }

    @Override
    public void clear() {
        delegate.clear();
        toolResultBuffer.clear();
        lastAiMessage = null;
    }

    private String extractFilePathFromToolMessage(ToolExecutionResultMessage msg) {
        if (msg == null) return null;
        String toolName = msg.toolName();
        if (toolName != null && toolName.startsWith("FILE:")) {
            return toolName.substring("FILE:".length());
        }

        // 如果 content 是 JSON 且包含 relativeFilePath
        try {
            Map<?, ?> map = objectMapper.readValue(msg.text(), Map.class);
            Object path = map.get("relativeFilePath");
            if (path instanceof String) return (String) path;
        } catch (Exception ignored) { }

        return null;
    }

    private String extractFilePathFromAiMessage(AiMessage msg) {
        if (msg == null || msg.toolExecutionRequests() == null) return null;
        for (ToolExecutionRequest req : msg.toolExecutionRequests()) {
            if (req != null && req.arguments() != null) {
                try {
                    Map<String, Object> argsMap = objectMapper.readValue(req.arguments(), Map.class);
                    Object path = argsMap.get("relativeFilePath");
                    if (path instanceof String) return (String) path;
                } catch (Exception e) {
                    log.warn("解析 AiMessage 中 arguments 失败，跳过: {}", req.arguments(), e);

                }
            }
        }
        return null;
    }
}
