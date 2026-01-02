package com.zriyo.aicodemother.core.handler;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.zriyo.aicodemother.event.ChatToVueCodeEvent;
import com.zriyo.aicodemother.model.dto.chat.ChatMessage;
import com.zriyo.aicodemother.model.entity.AiToolLog;
import com.zriyo.aicodemother.model.enums.ChatHistoryMessageTypeEnum;
import com.zriyo.aicodemother.model.enums.ToolAction;
import com.zriyo.aicodemother.model.message.*;
import com.zriyo.aicodemother.util.CodeBlockExtractor;
import jodd.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;

@Component
@Slf4j
public class VueProjectSseHandler implements CodeGenSseHandler {



    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;



    @Override
    public Flux<ServerSentEvent<Object>> handleStream(Flux<String> source, long appId, String codeGenType, Long userId, ChatMessage chatMessage) {
        Map<String, String> pendingTools = new HashMap<>();
        StringBuilder buffer = new StringBuilder();
        List<AiToolLog> aiToolLogs = new ArrayList<>();
        HashSet<String> toolIds = new HashSet<>();
        List<ChatMessage> chatMessages = new ArrayList<>();
        chatMessages.add(chatMessage);
        Flux<ServerSentEvent<Object>> mainFlux = source.concatMap(chunk -> {
            try {
                StreamMessage base = JSONUtil.toBean(chunk, StreamMessage.class);
                StreamMessageTypeEnum type = StreamMessageTypeEnum.getEnumByValue(base.getType());
                switch (type) {
                    case AI_RESPONSE -> {
                        AiResponseMessage ai = JSONUtil.toBean(chunk, AiResponseMessage.class);
                        buffer.append(ai.getMessageData().getData());
                        return Mono.just(ServerSentEvent.builder()
                                .event(StreamMessageTypeEnum.AI_RESPONSE.getValue())
                                .data(ai.getMessageData())
                                .build());
                    }
                    case TOOL_REQUEST -> {
                        ToolRequestMessage req = JSONUtil.toBean(chunk, ToolRequestMessage.class);
                        String toolId = req.getId();
                        if (Objects.nonNull(toolId) && !toolIds.contains(toolId)) {
                            toolIds.add(toolId);
                            String requestLine = "🔧 [正在调用工具] " + req.getName();
                            pendingTools.put(toolId, requestLine);
                            buffer.append(requestLine).append("\n");
                            return Mono.just(ServerSentEvent.builder()
                                    .event(StreamMessageTypeEnum.TOOL_REQUEST.getValue())
                                    .data(MessageData.builder()
                                            .data(requestLine)
                                            .type(StreamMessageTypeEnum.TOOL_REQUEST.getValue())
                                            .build()
                                    ).build());
                        }
                        return Mono.empty();
                    }
                    case TOOL_EXECUTED -> {
                        ToolExecutedMessage exec = JSONUtil.toBean(chunk, ToolExecutedMessage.class);
                        String toolId = exec.getId();
                        String toolName = exec.getName();
                        JSONObject args = JSONUtil.parseObj(exec.getArguments());
                        String path = args.getStr("relativeFilePath");
                        String suffix = FileUtil.getSuffix(path);
                        String content = args.getStr("content");
                        String description = args.getStr("description");
                        if (StringUtil.isNotBlank(path)) {
                            AiToolLog aiToolLog = new AiToolLog();
                            aiToolLog.setToolName(toolName);
                            aiToolLog.setFilePath(path);
                            aiToolLog.setAction(ToolAction.WRITE.getValue());
                            aiToolLog.setSummary(description);
                            aiToolLogs.add(aiToolLog);
                        }

                        String executedLine = String.format("[工具调用 ⚙️ ] 写入文件 %s\n```%s\n%s\n```", path, suffix, content);
                        String replaced = CodeBlockExtractor.replaceCodeBlocks(executedLine, appId, codeGenType);
                        String requestLine = pendingTools.getOrDefault(toolId, "[选择工具] " + toolName);
                        String sseData = requestLine + "\n" + replaced;
                        buffer.append(sseData).append("\n");
                        pendingTools.remove(toolId);

                        return Mono.just(ServerSentEvent.builder()
                                .event(StreamMessageTypeEnum.TOOL_EXECUTED.getValue())
                                .data(MessageData.builder()
                                        .data(sseData)
                                        .type(StreamMessageTypeEnum.TOOL_EXECUTED.getValue())
                                        .build()
                                ).build());
                    }
                    default -> Mono.empty();
                }
            } catch (Exception e) {
                return Mono.empty();
            }
            return Mono.empty();
        });

        // 心跳流，随主流结束而结束
        Flux<ServerSentEvent<Object>> heartbeat = Flux.interval(Duration.ofSeconds(6))
                .map(tick -> ServerSentEvent.builder()
                        .event("heartbeat")
                        .data("ping")
                        .build())
                .takeUntilOther(mainFlux);

        return Flux.merge(mainFlux, heartbeat)
                .onErrorContinue((throwable, o) -> log.warn("SSE流异常: {}", throwable.getMessage()))
                .concatWith(Mono.defer(() -> {
                    ChatMessage message = getMessage(appId, buffer.toString());
                    chatMessages.add( message);
                    applicationEventPublisher.publishEvent(new ChatToVueCodeEvent(this, aiToolLogs, userId, chatMessages));
                    return Mono.just(ServerSentEvent.builder()
                            .event(StreamMessageTypeEnum.AI_DONE.getValue())
                            .data(MessageData.doneOf())
                            .build());
                }));
    }

    private ChatMessage getMessage(long appId, String content) {
        ChatMessage msg = new ChatMessage();
        msg.setAppId(appId);
        msg.setMessage(content);
        msg.setMessageType(ChatHistoryMessageTypeEnum.AI.getValue());
        return msg;

    }
}
