package com.zriyo.aicodemother.core.handler;

import com.zriyo.aicodemother.event.ChatToVueCodeEvent;
import com.zriyo.aicodemother.model.dto.chat.ChatMessage;
import com.zriyo.aicodemother.model.entity.AiToolLog;
import com.zriyo.aicodemother.model.enums.ChatHistoryMessageTypeEnum;
import com.zriyo.aicodemother.model.message.AiResponseMessage;
import com.zriyo.aicodemother.model.message.MessageData;
import com.zriyo.aicodemother.model.message.StreamMessageTypeEnum;
import com.zriyo.aicodemother.util.CodeBlockExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Component
public class HtmlCodeGenSseHandler implements CodeGenSseHandler {

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;


    @Override
    public Flux<ServerSentEvent<Object>> handleStream(Flux<String> source, long appId, String codeGenType, Long userId, ChatMessage chatMessage) {
        List<ChatMessage> chatMessages = new ArrayList<>();
        chatMessages.add(chatMessage);
        StringBuffer str = new StringBuffer();
        return source.flatMap(full -> {
            str.append(full);
            AiResponseMessage data = new AiResponseMessage(full);
            return Mono.just(ServerSentEvent.builder()
                    .event(data.getType())
                    .data(data)
                    .build());
        }).concatWith(Mono.defer(() -> {
            String replaced = CodeBlockExtractor.replaceCodeBlocks(str.toString(), appId, codeGenType);
            ChatMessage aiMsg = new ChatMessage();
            aiMsg.setAppId(appId);
            aiMsg.setMessage(replaced);
            aiMsg.setMessageType(ChatHistoryMessageTypeEnum.AI.getValue());
            chatMessages.add(aiMsg);
            List<AiToolLog> aiToolLogs = new ArrayList<>();
            applicationEventPublisher.publishEvent(new ChatToVueCodeEvent(this,aiToolLogs , userId, chatMessages));
            ServerSentEvent<Object> doneEvent = ServerSentEvent.builder()
                    .event(StreamMessageTypeEnum.AI_DONE.getValue())
                    .data(MessageData.doneOf())
                    .build();
            return Mono.just(doneEvent);
        }));
    }
}
