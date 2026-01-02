package com.zriyo.aicodemother.util;

import com.zriyo.aicodemother.model.message.MessageData;
import com.zriyo.aicodemother.model.message.StreamMessageTypeEnum;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.FluxSink;

/**
 * 面向业务场景的 Server-Sent Event 构建工具类
 * 严格遵循项目现有的消息模型：StreamMessageTypeEnum + MessageData
 */
public class SseEventBuilder {

    /**
     * 构建标准业务 SSE 事件（使用 StreamMessageTypeEnum）
     */
    public static ServerSentEvent<Object> of(StreamMessageTypeEnum type, String data) {
        return ServerSentEvent.builder()
                .event(type.getValue())
                .data(MessageData.builder()
                        .type(type.getValue())
                        .data(data)
                        .build())
                .build();
    }
    /**
     * 构建标准业务 SSE 事件（使用 StreamMessageTypeEnum）
     */
    public static ServerSentEvent<Object> of(StreamMessageTypeEnum type) {
        return ServerSentEvent.builder()
                .event(type.getValue())
                .data(MessageData.builder()
                        .type(type.getValue())
                        .data(type.getText())
                        .build())
                .build();
    }

    /**
     * 构建 [DONE] 完成事件（特殊处理）
     */
    public static ServerSentEvent<Object> done() {
        return ServerSentEvent.builder()
                .event(StreamMessageTypeEnum.AI_DONE.getValue())
                .data(MessageData.doneOf())
                .build();
    }

    /**
     * 心跳事件（固定格式）
     */
    public static ServerSentEvent<Object> heartbeat() {
        return ServerSentEvent.builder()
                .event("heartbeat")
                .data("ping")
                .build();
    }


    /**
     * 向 sink 发送 AI 响应片段
     */
    public static void sendAiResponse(FluxSink<ServerSentEvent<Object>> sink, String data) {
        sink.next(of(StreamMessageTypeEnum.AI_RESPONSE, data));
    }

    /**
     * 向 sink 发送工具请求通知
     */
    public static void sendToolRequest(FluxSink<ServerSentEvent<Object>> sink, String data) {
        sink.next(of(StreamMessageTypeEnum.TOOL_REQUEST, data));
    }

    /**
     * 向 sink 发送工具执行结果
     */
    public static void sendToolExecuted(FluxSink<ServerSentEvent<Object>> sink, String data) {
        sink.next(of(StreamMessageTypeEnum.TOOL_EXECUTED, data));
    }

    /**
     * 发送完成事件
     */
    public static void sendDone(FluxSink<ServerSentEvent<Object>> sink) {
        sink.next(done());
    }
}
