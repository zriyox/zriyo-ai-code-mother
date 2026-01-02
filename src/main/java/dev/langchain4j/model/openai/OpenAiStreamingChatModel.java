package dev.langchain4j.model.openai;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.http.client.HttpClientBuilder;
import dev.langchain4j.internal.ExceptionMapper;
import dev.langchain4j.internal.Json;
import dev.langchain4j.internal.ToolExecutionRequestBuilder;
import dev.langchain4j.model.ModelProvider;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.request.DefaultChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openai.internal.OpenAiClient;
import dev.langchain4j.model.openai.internal.chat.*;
import dev.langchain4j.model.openai.internal.shared.StreamOptions;
import dev.langchain4j.model.openai.spi.OpenAiStreamingChatModelBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static dev.langchain4j.internal.InternalStreamingChatResponseHandlerUtils.withLoggingExceptions;
import static dev.langchain4j.internal.Utils.copy;
import static dev.langchain4j.internal.Utils.getOrDefault;
import static dev.langchain4j.model.ModelProvider.OPEN_AI;
import static dev.langchain4j.model.openai.internal.OpenAiUtils.*;
import static dev.langchain4j.spi.ServiceHelper.loadFactories;
import static java.time.Duration.ofSeconds;

/**
 * OpenAI Streaming Chat Model: 职责单一化，负责数据解析，不处理底层递归重试。
 */
public class OpenAiStreamingChatModel implements StreamingChatModel {

    private static final Logger log = LoggerFactory.getLogger(OpenAiStreamingChatModel.class);

    private final OpenAiClient client;
    private final OpenAiChatRequestParameters defaultRequestParameters;
    private final Boolean strictJsonSchema;
    private final Boolean strictTools;
    private final List<ChatModelListener> listeners;

    public OpenAiStreamingChatModel(OpenAiStreamingChatModelBuilder builder) {

        this.client = OpenAiClient.builder()
                .httpClientBuilder(builder.httpClientBuilder)
                .baseUrl(getOrDefault(builder.baseUrl, DEFAULT_OPENAI_URL))
                .apiKey(builder.apiKey)
                .organizationId(builder.organizationId)
                .projectId(builder.projectId)
                .connectTimeout(getOrDefault(builder.timeout, ofSeconds(15)))
                .readTimeout(getOrDefault(builder.timeout, ofSeconds(60)))
                .logRequests(getOrDefault(builder.logRequests, false))
                .logResponses(getOrDefault(builder.logResponses, false))
                .userAgent(DEFAULT_USER_AGENT)
                .customHeaders(builder.customHeaders)
                .build();

        ChatRequestParameters commonParameters = getOrDefault(builder.defaultRequestParameters, DefaultChatRequestParameters.EMPTY);

        OpenAiChatRequestParameters openAiParameters = (builder.defaultRequestParameters instanceof OpenAiChatRequestParameters)
                ? (OpenAiChatRequestParameters) builder.defaultRequestParameters
                : OpenAiChatRequestParameters.EMPTY;

        this.defaultRequestParameters = OpenAiChatRequestParameters.builder()
                .modelName(getOrDefault(builder.modelName, commonParameters.modelName()))
                .temperature(getOrDefault(builder.temperature, commonParameters.temperature()))
                .topP(getOrDefault(builder.topP, commonParameters.topP()))
                .frequencyPenalty(getOrDefault(builder.frequencyPenalty, commonParameters.frequencyPenalty()))
                .presencePenalty(getOrDefault(builder.presencePenalty, commonParameters.presencePenalty()))
                .maxOutputTokens(getOrDefault(builder.maxTokens, commonParameters.maxOutputTokens()))
                .stopSequences(getOrDefault(builder.stop, commonParameters.stopSequences()))
                .toolSpecifications(commonParameters.toolSpecifications())
                .toolChoice(commonParameters.toolChoice())
                .responseFormat(getOrDefault(fromOpenAiResponseFormat(builder.responseFormat), commonParameters.responseFormat()))
                .maxCompletionTokens(getOrDefault(builder.maxCompletionTokens, openAiParameters.maxCompletionTokens()))
                .logitBias(getOrDefault(builder.logitBias, openAiParameters.logitBias()))
                .parallelToolCalls(getOrDefault(builder.parallelToolCalls, openAiParameters.parallelToolCalls()))
                .seed(getOrDefault(builder.seed, openAiParameters.seed()))
                .user(getOrDefault(builder.user, openAiParameters.user()))
                .store(getOrDefault(builder.store, openAiParameters.store()))
                .metadata(getOrDefault(builder.metadata, openAiParameters.metadata()))
                .serviceTier(getOrDefault(builder.serviceTier, openAiParameters.serviceTier()))
                .reasoningEffort(openAiParameters.reasoningEffort())
                .build();

        this.strictJsonSchema = getOrDefault(builder.strictJsonSchema, false);
        this.strictTools = getOrDefault(builder.strictTools, false);
        this.listeners = copy(builder.listeners);
    }

    @Override
    public OpenAiChatRequestParameters defaultRequestParameters() {
        return defaultRequestParameters;
    }

    @Override
    public void doChat(ChatRequest chatRequest, StreamingChatResponseHandler handler) {
//        List<ChatMessage> messages = chatRequest.messages();
//        log.info("Sending {} messages to LLM:", messages.size());
//        for (ChatMessage m : messages) {
//            log.info("  - {}: {}", m.getClass().getSimpleName(), m);
//        }

        OpenAiChatRequestParameters parameters = (OpenAiChatRequestParameters) chatRequest.parameters();
        validate(parameters);

        ChatCompletionRequest openAiRequest = toOpenAiChatRequest(chatRequest, parameters, strictTools, strictJsonSchema)
                .stream(true)
                .streamOptions(StreamOptions.builder().includeUsage(true).build())
                .build();

        OpenAiStreamingResponseBuilder openAiResponseBuilder = new OpenAiStreamingResponseBuilder();
        ToolExecutionRequestBuilder toolBuilder = new ToolExecutionRequestBuilder();

        client.chatCompletion(openAiRequest)
                .onPartialResponse(partialResponse -> {
                    openAiResponseBuilder.append(partialResponse);
                    handle(partialResponse, toolBuilder, handler);
                })
                .onComplete(() -> {
                    if (toolBuilder.hasToolExecutionRequests()) {
                        try {
                            handler.onCompleteToolExecutionRequest(toolBuilder.index(), toolBuilder.build());
                        } catch (Exception e) {
                            log.error("Error completing tool execution", e);
                            withLoggingExceptions(() -> handler.onError(e));
                        }
                    }
                    ChatResponse chatResponse = openAiResponseBuilder.build();
                    try {
                        handler.onCompleteResponse(chatResponse);
                    } catch (Exception e) {
                        log.error("Error completing chat response", e);
                        withLoggingExceptions(() -> handler.onError(e));
                    }
                })
                .onError(throwable -> {
                    // 🔹 底层不再处理重试，仅做异常转换并抛出给 Proxy/Handler 处理
                    RuntimeException mappedException = ExceptionMapper.DEFAULT.mapException(throwable);
                    log.error("Chat streaming HTTP error: {}", mappedException.getMessage());
                    withLoggingExceptions(() -> handler.onError(mappedException));
                })
                .execute();
    }

    private static void handle(ChatCompletionResponse partialResponse,
                               ToolExecutionRequestBuilder toolBuilder,
                               StreamingChatResponseHandler handler) {
        if (partialResponse == null) return;

        List<ChatCompletionChoice> choices = partialResponse.choices();
        if (choices == null || choices.isEmpty()) return;

        Delta delta = choices.get(0).delta();
        if (delta == null) return;

        // 1. 处理文本
        String content = delta.content();
        if (content != null && !content.isEmpty()) {
            try {
                handler.onPartialResponse(content);
            } catch (Exception e) {
                log.error("Error on partial response", e);
                withLoggingExceptions(() -> handler.onError(e));
            }
        }

        // 2. 处理工具调用
        List<ToolCall> toolCalls = delta.toolCalls();
        if (toolCalls != null) {
            for (ToolCall toolCall : toolCalls) {
                int index = toolCall.index();
                if (toolBuilder.index() != index) {
                    try {
                        if (toolBuilder.hasToolExecutionRequests()) {
                            handler.onCompleteToolExecutionRequest(toolBuilder.index(), toolBuilder.build());
                        }
                    } catch (Exception e) {
                        log.error("Error completing tool execution during partial", e);
                        withLoggingExceptions(() -> handler.onError(e));
                    }
                    toolBuilder.updateIndex(index);
                }

                if (toolCall.id() != null) toolBuilder.updateId(toolCall.id());
                if (toolCall.function() != null) {
                    if (toolCall.function().name() != null) toolBuilder.updateName(toolCall.function().name());

                    String partialArgs = toolCall.function().arguments();
                    if (partialArgs != null && !partialArgs.isEmpty()) {
                        // 使用你现有的安全包装逻辑（保持不动）
                        String safeArgs = safeWrapArguments(partialArgs);
                        toolBuilder.appendArguments(safeArgs);

                        ToolExecutionRequest partialRequest = ToolExecutionRequest.builder()
                                .id(toolBuilder.id())
                                .name(toolBuilder.name())
                                .arguments(safeArgs)
                                .build();
                        try {
                            handler.onPartialToolExecutionRequest(index, partialRequest);
                        } catch (Exception e) {
                            log.error("Error on partial tool execution request", e);
                            withLoggingExceptions(() -> handler.onError(e));
                        }
                    }
                }
            }
        }
    }

    private static String safeWrapArguments(String args) {
        if (args == null || args.isEmpty()) return "{\"content\":\"\"}";
        String trimmed = args.trim();

        if ((trimmed.startsWith("{") && trimmed.endsWith("}")) ||
                (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
            try {
                Json.fromJson(trimmed, Object.class);
                return trimmed;
            } catch (Exception ignored) {
            }
        }

        if (trimmed.startsWith("{") && !trimmed.endsWith("}")) {
            long quoteCount = trimmed.chars().filter(ch -> ch == '"').count();
            if (quoteCount % 2 != 0) {
                trimmed += "\"";
            }
            trimmed += "}";
        } else if (trimmed.startsWith("[") && !trimmed.endsWith("]")) {
            trimmed += "]";
        }

        return "{\"content\":\"" + escapeJsonString(trimmed) + "\"}";
    }

    private static String escapeJsonString(String input) {
        if (input == null) return null;
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c <= 0x1F) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        return sb.toString();
    }

    @Override
    public List<ChatModelListener> listeners() {
        return listeners;
    }

    @Override
    public ModelProvider provider() {
        return OPEN_AI;
    }

    public static OpenAiStreamingChatModelBuilder builder() {
        for (OpenAiStreamingChatModelBuilderFactory factory : loadFactories(OpenAiStreamingChatModelBuilderFactory.class)) {
            return factory.get();
        }
        return new OpenAiStreamingChatModelBuilder();
    }

    public static class OpenAiStreamingChatModelBuilder {
        private HttpClientBuilder httpClientBuilder;
        private String baseUrl;
        private String apiKey;
        private String organizationId;
        private String projectId;
        private ChatRequestParameters defaultRequestParameters;
        private String modelName;
        private Double temperature;
        private Double topP;
        private List<String> stop;
        private Integer maxTokens;
        private Integer maxCompletionTokens;
        private Double presencePenalty;
        private Double frequencyPenalty;
        private Map<String, Integer> logitBias;
        private String responseFormat;
        private Boolean strictJsonSchema;
        private Integer seed;
        private String user;
        private Boolean strictTools;
        private Boolean parallelToolCalls;
        private Boolean store;
        private Map<String, String> metadata;
        private String serviceTier;
        private Duration timeout;
        private Boolean logRequests;
        private Boolean logResponses;
        private Map<String, String> customHeaders;
        private List<ChatModelListener> listeners;

        public OpenAiStreamingChatModelBuilder() {}

        public OpenAiStreamingChatModelBuilder httpClientBuilder(HttpClientBuilder httpClientBuilder) {
            this.httpClientBuilder = httpClientBuilder;
            return this;
        }

        public OpenAiStreamingChatModelBuilder defaultRequestParameters(ChatRequestParameters parameters) {
            this.defaultRequestParameters = parameters;
            return this;
        }

        public OpenAiStreamingChatModelBuilder modelName(String modelName) {
            this.modelName = modelName;
            return this;
        }

        public OpenAiStreamingChatModelBuilder modelName(OpenAiChatModelName modelName) {
            this.modelName = modelName.toString();
            return this;
        }

        public OpenAiStreamingChatModelBuilder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public OpenAiStreamingChatModelBuilder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public OpenAiStreamingChatModelBuilder organizationId(String organizationId) {
            this.organizationId = organizationId;
            return this;
        }

        public OpenAiStreamingChatModelBuilder projectId(String projectId) {
            this.projectId = projectId;
            return this;
        }

        public OpenAiStreamingChatModelBuilder temperature(Double temperature) {
            this.temperature = temperature;
            return this;
        }

        public OpenAiStreamingChatModelBuilder topP(Double topP) {
            this.topP = topP;
            return this;
        }

        public OpenAiStreamingChatModelBuilder stop(List<String> stop) {
            this.stop = stop;
            return this;
        }

        public OpenAiStreamingChatModelBuilder maxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public OpenAiStreamingChatModelBuilder maxCompletionTokens(Integer maxCompletionTokens) {
            this.maxCompletionTokens = maxCompletionTokens;
            return this;
        }

        public OpenAiStreamingChatModelBuilder presencePenalty(Double presencePenalty) {
            this.presencePenalty = presencePenalty;
            return this;
        }

        public OpenAiStreamingChatModelBuilder frequencyPenalty(Double frequencyPenalty) {
            this.frequencyPenalty = frequencyPenalty;
            return this;
        }

        public OpenAiStreamingChatModelBuilder logitBias(Map<String, Integer> logitBias) {
            this.logitBias = logitBias;
            return this;
        }

        public OpenAiStreamingChatModelBuilder responseFormat(String responseFormat) {
            this.responseFormat = responseFormat;
            return this;
        }

        public OpenAiStreamingChatModelBuilder strictJsonSchema(Boolean strictJsonSchema) {
            this.strictJsonSchema = strictJsonSchema;
            return this;
        }

        public OpenAiStreamingChatModelBuilder seed(Integer seed) {
            this.seed = seed;
            return this;
        }

        public OpenAiStreamingChatModelBuilder user(String user) {
            this.user = user;
            return this;
        }

        public OpenAiStreamingChatModelBuilder strictTools(Boolean strictTools) {
            this.strictTools = strictTools;
            return this;
        }

        public OpenAiStreamingChatModelBuilder parallelToolCalls(Boolean parallelToolCalls) {
            this.parallelToolCalls = parallelToolCalls;
            return this;
        }

        public OpenAiStreamingChatModelBuilder store(Boolean store) {
            this.store = store;
            return this;
        }

        public OpenAiStreamingChatModelBuilder metadata(Map<String, String> metadata) {
            this.metadata = metadata;
            return this;
        }

        public OpenAiStreamingChatModelBuilder serviceTier(String serviceTier) {
            this.serviceTier = serviceTier;
            return this;
        }

        public OpenAiStreamingChatModelBuilder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public OpenAiStreamingChatModelBuilder logRequests(Boolean logRequests) {
            this.logRequests = logRequests;
            return this;
        }

        public OpenAiStreamingChatModelBuilder logResponses(Boolean logResponses) {
            this.logResponses = logResponses;
            return this;
        }

        public OpenAiStreamingChatModelBuilder customHeaders(Map<String, String> customHeaders) {
            this.customHeaders = customHeaders;
            return this;
        }

        public OpenAiStreamingChatModelBuilder listeners(List<ChatModelListener> listeners) {
            this.listeners = listeners;
            return this;
        }

        public OpenAiStreamingChatModel build() {
            return new OpenAiStreamingChatModel(this);
        }
    }
}
