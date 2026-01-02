package dev.langchain4j.service;

import dev.langchain4j.Internal;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.guardrail.ChatExecutor;
import dev.langchain4j.guardrail.GuardrailRequestParams;
import dev.langchain4j.guardrail.OutputGuardrailRequest;
import dev.langchain4j.internal.Json;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.service.tool.ToolExecution;
import dev.langchain4j.service.tool.ToolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static dev.langchain4j.internal.Utils.copy;
import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;

/**
 * Handles response from a language model for AI Service that is streamed token-by-token.
 *
 * <p>When output guardrails are enabled, partial responses are buffered and only released
 * after the full response passes guardrail validation. This prevents leaking tool call
 * arguments or unsafe content during streaming.</p>
 */
@Internal
class AiServiceStreamingResponseHandler implements StreamingChatResponseHandler {

    private static final Logger LOG = LoggerFactory.getLogger(AiServiceStreamingResponseHandler.class);

    private final ChatExecutor chatExecutor;
    private final AiServiceContext context;
    private final Object memoryId;
    private final GuardrailRequestParams commonGuardrailParams;
    private final Object methodKey;

    private final Consumer<String> partialResponseHandler;
    private final BiConsumer<Integer, ToolExecutionRequest> partialToolExecutionRequestHandler;
    private final BiConsumer<Integer, ToolExecutionRequest> completeToolExecutionRequestHandler;
    private final Consumer<ToolExecution> toolExecutionHandler;
    private final Consumer<ChatResponse> completeResponseHandler;
    private final Consumer<Throwable> errorHandler;

    private final ChatMemory temporaryMemory;
    private final TokenUsage tokenUsage;

    private final List<ToolSpecification> toolSpecifications;
    private final Map<String, ToolExecutor> toolExecutors;

    // Buffer for AI text (only used when output guardrails are active)
    private final StringBuilder responseBuffer = new StringBuilder();
    private final boolean hasOutputGuardrails;

    AiServiceStreamingResponseHandler(
            ChatExecutor chatExecutor,
            AiServiceContext context,
            Object memoryId,
            Consumer<String> partialResponseHandler,
            BiConsumer<Integer, ToolExecutionRequest> partialToolExecutionRequestHandler,
            BiConsumer<Integer, ToolExecutionRequest> completeToolExecutionRequestHandler,
            Consumer<ToolExecution> toolExecutionHandler,
            Consumer<ChatResponse> completeResponseHandler,
            Consumer<Throwable> errorHandler,
            ChatMemory temporaryMemory,
            TokenUsage tokenUsage,
            List<ToolSpecification> toolSpecifications,
            Map<String, ToolExecutor> toolExecutors,
            GuardrailRequestParams commonGuardrailParams,
            Object methodKey
    ) {
        this.chatExecutor = ensureNotNull(chatExecutor, "chatExecutor");
        this.context = ensureNotNull(context, "context");
        this.memoryId = ensureNotNull(memoryId, "memoryId");
        this.methodKey = methodKey;

        this.partialResponseHandler = ensureNotNull(partialResponseHandler, "partialResponseHandler");
        this.partialToolExecutionRequestHandler = partialToolExecutionRequestHandler;
        this.completeToolExecutionRequestHandler = completeToolExecutionRequestHandler;
        this.completeResponseHandler = completeResponseHandler;
        this.toolExecutionHandler = toolExecutionHandler;
        this.errorHandler = errorHandler;

        this.temporaryMemory = temporaryMemory;
        this.tokenUsage = ensureNotNull(tokenUsage, "tokenUsage");
        this.commonGuardrailParams = commonGuardrailParams;

        this.toolSpecifications = copy(toolSpecifications);
        this.toolExecutors = copy(toolExecutors);
        this.hasOutputGuardrails = context.guardrailService().hasOutputGuardrails(methodKey);
    }

    @Override
    public void onPartialResponse(String partialResponse) {
        if (hasOutputGuardrails) {
            responseBuffer.append(partialResponse);
        } else {
            partialResponseHandler.accept(partialResponse);
        }
    }

    @Override
    public void onPartialToolExecutionRequest(int index, ToolExecutionRequest partialToolExecutionRequest) {
        if (partialToolExecutionRequestHandler != null) {
            partialToolExecutionRequestHandler.accept(index, partialToolExecutionRequest);
        }
    }

    @Override
    public void onCompleteResponse(ChatResponse completeResponse) {
        AiMessage aiMessage = completeResponse.aiMessage();
        addToMemory(aiMessage);

        if (aiMessage.hasToolExecutionRequests()) {
            handleToolCalls(aiMessage);
        } else {
            handleTextResponse(completeResponse, aiMessage);
        }
    }

    private void handleToolCalls(AiMessage aiMessage) {
        for (ToolExecutionRequest originalRequest : aiMessage.toolExecutionRequests()) {
            ToolExecutionRequest safeRequest = safeWrapArguments(originalRequest);

            ToolExecutor executor = toolExecutors.get(safeRequest.name());
            if (executor == null) {
                LOG.warn("No executor found for tool: {}", safeRequest.name());
                continue;
            }

            String result;
            try {
                result = executor.execute(safeRequest, memoryId);
            } catch (Exception e) {
                LOG.error("Error executing tool '{}': {}", safeRequest.name(), e.getMessage(), e);
                result = "Tool execution failed: " + e.getMessage();
            }

            // 确保 result 是字符串（如果不是，转成 JSON）
            String resultText = (result instanceof String)
                    ? (String) result
                    : (result == null ? "" : dev.langchain4j.internal.Json.toJson(result));

            // 手动构造：id = tool_call_id, toolName = 工具名, text = 结果内容
            ToolExecutionResultMessage message = new ToolExecutionResultMessage(
                    safeRequest.id(),      // ← 这就是 OpenAI 的 tool_call_id
                    safeRequest.name(),    // 工具名称（可选，但保留无害）
                    resultText             // 必须是字符串
            );
            addToMemory(message);

            if (toolExecutionHandler != null) {
                ToolExecution te = ToolExecution.builder()
                        .request(safeRequest)
                        .result(result)
                        .build();
                toolExecutionHandler.accept(te);
            }
        }

        // Recursive follow-up
        ChatRequest chatRequest = ChatRequest.builder()
                .messages(messagesToSend(memoryId))
                .toolSpecifications(toolSpecifications)
                .build();

        var handler = new AiServiceStreamingResponseHandler(
                chatExecutor,
                context,
                memoryId,
                partialResponseHandler,
                partialToolExecutionRequestHandler,
                completeToolExecutionRequestHandler,
                toolExecutionHandler,
                completeResponseHandler,
                errorHandler,
                temporaryMemory,
                tokenUsage, // tokenUsage sum handled in next response
                toolSpecifications,
                toolExecutors,
                commonGuardrailParams,
                methodKey
        );

        context.streamingChatModel.chat(chatRequest, handler);
    }

    private void handleTextResponse(ChatResponse completeResponse, AiMessage aiMessage) {
        ChatResponse finalResponse;

        if (hasOutputGuardrails) {
            String fullText = responseBuffer.toString();
            responseBuffer.setLength(0);

            AiMessage msg = AiMessage.from(fullText);
            ChatResponse original = ChatResponse.builder()
                    .aiMessage(msg)
                    .metadata(completeResponse.metadata().toBuilder()
                            .tokenUsage(tokenUsage.add(completeResponse.metadata().tokenUsage()))
                            .build())
                    .build();

            if (commonGuardrailParams != null) {
                var newParams = GuardrailRequestParams.builder()
                        .chatMemory(getMemory())
                        .augmentationResult(commonGuardrailParams.augmentationResult())
                        .userMessageTemplate(commonGuardrailParams.userMessageTemplate())
                        .variables(commonGuardrailParams.variables())
                        .build();

                var outputParams = OutputGuardrailRequest.builder()
                        .responseFromLLM(original)
                        .chatExecutor(chatExecutor)
                        .requestParams(newParams)
                        .build();

                ChatResponse guarded = context.guardrailService().executeGuardrails(methodKey, outputParams);
                finalResponse = guarded != null ? guarded : original;
            } else {
                finalResponse = original;
            }

            if (completeResponseHandler != null) {
                completeResponseHandler.accept(finalResponse);
            }

        } else {
            finalResponse = ChatResponse.builder()
                    .aiMessage(aiMessage)
                    .metadata(completeResponse.metadata().toBuilder()
                            .tokenUsage(tokenUsage.add(completeResponse.metadata().tokenUsage()))
                            .build())
                    .build();

            if (completeResponseHandler != null) {
                completeResponseHandler.accept(finalResponse);
            }
        }
    }

    private ToolExecutionRequest safeWrapArguments(ToolExecutionRequest request) {
        String args = request.arguments();

        // 空或 null 参数，直接包装成 {"content":" "}
        if (args == null || args.trim().isEmpty()) {
            return ToolExecutionRequest.builder()
                    .id(request.id())
                    .name(request.name())
                    .arguments("{\"content\":\" \"}")
                    .build();
        }

        String trimmed = args.trim();
        // 已经是合法 JSON 对象或数组
        if ((trimmed.startsWith("{") && trimmed.endsWith("}")) ||
                (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
            try {
                Json.fromJson(args, Object.class);
                return request; // 合法 JSON 保留
            } catch (Exception ignored) {
                // 不是合法 JSON，继续处理
            }
        }

        // 非 JSON 文本，包装成 {"content":"..."}
        String escaped = escapeJsonString(args);
        LOG.debug("Wrapping non-JSON text as JSON: {}", escaped);
        String wrapped = "{\"content\":\"" + escaped + "\"}";
        return ToolExecutionRequest.builder()
                .id(request.id())
                .name(request.name())
                .arguments(wrapped)
                .build();
    }

    private static String escapeJsonString(String input) {
        if (input == null) return null;
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c <= 0x1F) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        return sb.toString();
    }

    private ChatMemory getMemory() {
        return context.hasChatMemory() ? context.chatMemoryService.getOrCreateChatMemory(memoryId) : temporaryMemory;
    }

    private void addToMemory(ChatMessage chatMessage) {
        getMemory().add(chatMessage);
    }

    private List<ChatMessage> messagesToSend(Object memoryId) {
        return getMemory().messages();
    }

    @Override
    public void onError(Throwable error) {
        if (errorHandler != null) {
            try {
                errorHandler.accept(error);
            } catch (Exception e) {
                LOG.error("While handling the following error...", error);
                LOG.error("...the following error happened", e);
            }
        } else {
            LOG.warn("Ignored error", error);
        }
    }
}
