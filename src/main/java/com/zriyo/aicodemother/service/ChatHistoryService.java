package com.zriyo.aicodemother.service;

import com.mybatisflex.core.service.IService;
import com.zriyo.aicodemother.core.pipeline.GenerationContext;
import com.zriyo.aicodemother.model.dto.ProjectSkeletonDTO;
import com.zriyo.aicodemother.model.dto.chat.ChatHistoryQueryRequest;
import com.zriyo.aicodemother.model.dto.chat.ChatMessage;
import com.zriyo.aicodemother.model.entity.ChatHistory;
import com.zriyo.aicodemother.model.enums.AiCodeGenStage;
import com.zriyo.aicodemother.model.enums.ChatHistoryMessageTypeEnum;
import com.zriyo.aicodemother.model.vo.ChatHistoryQueryVo;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.util.List;

/**
 * 对话历史 服务层。
 *
 */
public interface ChatHistoryService extends IService<ChatHistory> {

    Long addChatMessage(ChatMessage chatMessage, Long userId);

    boolean deleteByAppId(Long appId);

    List<ChatHistoryQueryVo> ChatHistoryQueryList(ChatHistoryQueryRequest request, Long userId);


    int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount);

    List<Long> addChatMessage(List<ChatMessage> chatMessage, Long userId);

    int loadChatHistoryToToolMemoryV2(Long appId,String filePath, MessageWindowChatMemory chatMemory, String filePathProject);

    int loadErrorChatHistoryToToolMemoryV2(Long appid, String redisKey, MessageWindowChatMemory chatMemory, String filePathProject);

    ChatMessage buildUserInfo(Long appId, String message, ChatHistoryMessageTypeEnum messageType, boolean isUserVisible);

    ProjectSkeletonDTO getLastSkeletonByType(Long appId, Long userId, String value, GenerationContext context);

    void updateSkeleton(ProjectSkeletonDTO skeleton, Long Id);

    void deleteSkeleton(Long appId, AiCodeGenStage aiCodeGenStage);

}
