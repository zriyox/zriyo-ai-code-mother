package com.zriyo.aicodemother.event.listener;

import com.zriyo.aicodemother.config.AsyncConfig;
import com.zriyo.aicodemother.event.ChatToVueCodeEvent;
import com.zriyo.aicodemother.model.dto.chat.ChatMessage;
import com.zriyo.aicodemother.model.entity.AiToolLog;
import com.zriyo.aicodemother.service.AiToolLogService;
import com.zriyo.aicodemother.service.ChatHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Component
@Slf4j
@RequiredArgsConstructor
public class ChatToVueCodeListener  {

    private final ChatHistoryService chatHistoryService;

    private final AiToolLogService aiToolLogService;

    /**
     *  对话消息入库
     */
    @EventListener(classes = ChatToVueCodeEvent.class)
    @Async(AsyncConfig.AI_ASYNC_EXECUTOR)
    @Transactional
    public void saveChatMessageOnEvent(ChatToVueCodeEvent event) {
        List<ChatMessage> chatMessages = event.getChatMessages();
        List<AiToolLog> aiToolLogs = event.getAiToolLogs();
        Long userId = event.getUserId();
        List<Long> id = chatHistoryService.addChatMessage(chatMessages, userId);
        if (!aiToolLogs.isEmpty()) {
            if (!id.isEmpty()){
                aiToolLogs.stream()
                        .filter(Objects::nonNull)
                        .forEach(aiToolLog -> {
                    aiToolLog.setAiMessageId(id.getFirst());
                });
            }
            aiToolLogService.saveBatch(aiToolLogs);
        }
    }
}
