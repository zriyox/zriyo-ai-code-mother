package com.zriyo.aicodemother.core.handler;

import com.zriyo.aicodemother.model.dto.chat.ChatMessage;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

public interface CodeGenSseHandler {
    /**
     * 将原始 AI 流转换成 SSE 流
     */
    Flux<ServerSentEvent<Object>> handleStream(Flux<String> source, long appId, String codeGenType, Long userId, ChatMessage chatMessage);
}
