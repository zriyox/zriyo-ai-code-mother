package com.zriyo.aicodemother.event;

import com.zriyo.aicodemother.model.dto.chat.ChatMessage;
import com.zriyo.aicodemother.model.entity.AiToolLog;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

@Getter
public class ChatToVueCodeEvent extends ApplicationEvent {

    private final List<AiToolLog> aiToolLogs;
    private final Long userId;
    private final List<ChatMessage> chatMessages;

    public ChatToVueCodeEvent(Object source, List<AiToolLog> aiToolLogs, Long userId, List<ChatMessage> chatMessages) {
        super(source);
        this.aiToolLogs = aiToolLogs;
        this.userId = userId;
        this.chatMessages = chatMessages;

    }
}
