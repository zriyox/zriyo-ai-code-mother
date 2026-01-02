package com.zriyo.aicodemother.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.util.UpdateEntity;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.zriyo.aicodemother.core.pipeline.GenerationContext;
import com.zriyo.aicodemother.exception.BusinessException;
import com.zriyo.aicodemother.exception.ErrorCode;
import com.zriyo.aicodemother.exception.ThrowUtils;
import com.zriyo.aicodemother.mapper.ChatHistoryMapper;
import com.zriyo.aicodemother.model.dto.ProjectSkeletonDTO;
import com.zriyo.aicodemother.model.dto.chat.ChatHistoryQueryRequest;
import com.zriyo.aicodemother.model.dto.chat.ChatMessage;
import com.zriyo.aicodemother.model.entity.ChatHistory;
import com.zriyo.aicodemother.model.enums.AiCodeGenStage;
import com.zriyo.aicodemother.model.enums.ChatHistoryMessageTypeEnum;
import com.zriyo.aicodemother.model.vo.ChatHistoryQueryVo;
import com.zriyo.aicodemother.service.ChatHistoryService;
import com.zriyo.aicodemother.util.RedisUtils;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 对话历史 服务层实现。
 *
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory> implements ChatHistoryService {
    private final ChatHistoryMapper chatHistoryMapper;

    private final ObjectMapper objectMapper;

    @Override
    public Long addChatMessage(ChatMessage chatMessage, Long userId) {
        ChatHistory chatHistory = saveOneChatMessage(chatMessage, userId);
        return chatHistory.getId();
    }

    private ChatHistory saveOneChatMessage(ChatMessage chatMessage, Long userId) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        String messageType = chatMessage.getMessageType();
        // 验证消息类型是否有效
        ChatHistoryMessageTypeEnum messageTypeEnum = ChatHistoryMessageTypeEnum.getEnumByValue(messageType);
        ThrowUtils.throwIf(messageTypeEnum == null, ErrorCode.PARAMS_ERROR, "不支持的消息类型: " + messageType);
        ChatHistory chatHistory = ChatHistory.builder()
                .appId(chatMessage.getAppId())
                .message(chatMessage.getMessage())
                .messageType(messageType)
                .userId(userId)
                .userVisible(chatMessage.getUserVisible())
                .build();
        chatMessage.setId(chatHistory.getId());
        boolean save = this.save(chatHistory);
        if (!save) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        return chatHistory;
    }

    @Override
    public boolean deleteByAppId(Long appId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("appId", appId);
        return this.remove(queryWrapper);
    }

    @Override
    public List<ChatHistoryQueryVo> ChatHistoryQueryList(ChatHistoryQueryRequest request, Long userId) {
        List<ChatHistoryQueryVo> list = chatHistoryMapper.ChatHistoryQueryList(request, userId);
        return list;
    }

    @Override
    public int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount) {
        try {
            // 直接构造查询条件，起始点为 1 而不是 0，用于排除最新的用户消息
            QueryWrapper queryWrapper = QueryWrapper.create()
                    .eq(ChatHistory::getAppId, appId)
                    .eq(ChatHistory::getUserVisible, 1)
                    .orderBy(ChatHistory::getCreateTime, false)
                    .limit(1, maxCount);
            List<ChatHistory> historyList = this.list(queryWrapper);
            if (CollUtil.isEmpty(historyList)) {
                return 0;
            }
            // 反转列表，确保按时间正序（老的在前，新的在后）
            historyList = historyList.reversed();
            // 按时间顺序添加到记忆中
            int loadedCount = 0;
            // 先清理历史缓存，防止重复加载
            chatMemory.clear();
            for (ChatHistory history : historyList) {
                if (ChatHistoryMessageTypeEnum.USER.getValue().equals(history.getMessageType())) {
                    chatMemory.add(UserMessage.from(history.getMessage()));
                    loadedCount++;
                } else if (ChatHistoryMessageTypeEnum.AI.getValue().equals(history.getMessageType())) {
                    chatMemory.add(AiMessage.from(history.getMessage()));
                    loadedCount++;
                }
            }
            log.info("成功为 appId: {} 加载了 {} 条历史对话", appId, loadedCount);
            return loadedCount;
        } catch (Exception e) {
            log.error("加载历史对话失败，appId: {}, error: {}", appId, e.getMessage(), e);
            // 加载失败不影响系统运行，只是没有历史上下文
            return 0;
        }
    }

    @Override
    public List<Long> addChatMessage(List<ChatMessage> chatMessages, Long userId) {
        List<ChatHistory> chatHistories = builderChatHistory(chatMessages, userId);
        return chatHistories.stream().map(chatHistory -> {
            if (chatHistory.getMessageType().equals(ChatHistoryMessageTypeEnum.AI.getValue())) {
                return chatHistory.getId();
            }
            return null;
        }).toList();
    }

    @Override
    public int loadChatHistoryToToolMemoryV2(Long appid, String redisKey, MessageWindowChatMemory chatMemory, String filePathProject) {
        int validCount = 0;
        chatMemory.clear();

        // 1. 加载聊天历史（仅此方法有）
        try {
            QueryWrapper queryWrapper = QueryWrapper.create()
                    .eq(ChatHistory::getAppId, appid)
                    .eq(ChatHistory::getUserVisible, 1)
                    .in(ChatHistory::getMessageType,
                            ChatHistoryMessageTypeEnum.USER.getValue(),
                            ChatHistoryMessageTypeEnum.AI.getValue(),
                            ChatHistoryMessageTypeEnum.TOOL.getValue())
                    .orderBy(ChatHistory::getCreateTime, false)
                    .limit(0, 20);
            List<ChatHistory> historyList = this.list(queryWrapper);
            if (CollUtil.isNotEmpty(historyList)) {
                historyList = historyList.reversed();
                for (ChatHistory history : historyList) {
                    switch (Objects.requireNonNull(ChatHistoryMessageTypeEnum.getEnumByValue(history.getMessageType()))) {
                        case USER -> chatMemory.add(UserMessage.from(history.getMessage()));
                        case TOOL -> chatMemory.add(AiMessage.from("【工具执行摘要】\n" + history.getMessage()));
                        case AI -> chatMemory.add(AiMessage.from(history.getMessage()));
                    }
                }
                validCount += historyList.size();
                log.info("加载历史上下文 {} 条", historyList.size());
            }
        } catch (Exception e) {
            log.error("加载历史上下文失败: {}", e.getMessage(), e);
        }

        // 2. 公共部分：骨架 + 依赖文件
        validCount += loadProjectContextIntoMemory(appid, redisKey, chatMemory, filePathProject,true);

        return validCount;
    }

    @Override
    public int loadErrorChatHistoryToToolMemoryV2(Long appid, String redisKey, MessageWindowChatMemory chatMemory, String filePathProject) {
        int validCount = 0;
        chatMemory.clear();
        // 2. 公共部分：骨架 + 依赖文件
        validCount += loadProjectContextIntoMemory(appid, redisKey, chatMemory, filePathProject,false);

        return validCount;
    }

    /**
     * 统一加载逻辑
     * @param includeSkeleton 是否注入项目骨架结构
     */
    private int loadProjectContextIntoMemory(Long appid, String redisKey, MessageWindowChatMemory chatMemory, String filePathProject, boolean includeSkeleton) {
        final String FILE_PATH_IMPORT = "FILE_PATH_IMPORT:";
        List<String> relativeFilePaths = RedisUtils.getCacheObject(FILE_PATH_IMPORT + redisKey);
        if (relativeFilePaths == null) {
            relativeFilePaths = new ArrayList<>();
        }

        int validCount = 0;
        log.info("当前生成文件: {}, 是否加载骨架: {}", redisKey, includeSkeleton);
        String projectRoot = com.zriyo.aicodemother.util.CodeOutputManager.getCodeOutputBaseDir().toString() + "/";

        // --- 1. 条件加载项目骨架（SKELETON） ---
        if (includeSkeleton) {
            QueryWrapper eq = QueryWrapper.create()
                    .eq(ChatHistory::getAppId, appid)
                    .eq(ChatHistory::getMessageType, ChatHistoryMessageTypeEnum.SKELETON.getValue());
            ChatHistory chatHistory = chatHistoryMapper.selectOneByQuery(eq);

            if (Objects.nonNull(chatHistory) && StrUtil.isNotEmpty(chatHistory.getMessage())) {
                String skeletonContext = "【当前项目文件快照 (Read-Only)】\n" +
                        "这是当前项目的骨架结构，请根据此结构生成文件，严禁虚构路径：\n" +
                        chatHistory.getMessage();
                chatMemory.add(UserMessage.from(skeletonContext));
                validCount++;
                log.info("已注入项目骨架快照");
            }
        }

        // --- 2. 加载依赖文件（逻辑保持不变） ---
        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append("[Reference Source Code Content]\n");
        boolean hasValidFile = false;
        int fileIndex = 0;

        for (String relativePath : relativeFilePaths) {
            if (StrUtil.isEmpty(relativePath)) continue;
            try {
                Path fullPath = Paths.get(projectRoot, filePathProject, relativePath);
                if (!Files.exists(fullPath)) {
                    log.warn("参考文件不存在: {}", fullPath);
                    continue;
                }

                String content = Files.readString(fullPath);
                fileIndex++;

                String processedContent;
                // 策略：前3个文件全量，后续文件脱水/截断
                if (fileIndex <= 3) {
                    processedContent = content;
                } else {
                    if (relativePath.endsWith(".vue")) {
                        processedContent = pruneVueContent(content);
                        log.info("对文件 [{}] 执行 Vue 脱水", relativePath);
                    } else {
                        processedContent = StrUtil.maxLength(content, 1000) + "\n... [Content truncated]";
                    }
                }

                // 处理 Handlebars/Mustache 转义
                processedContent = processedContent.replace("{{", "\\{\\{").replace("}}", "\\}\\}");
                contextBuilder.append("\n--- File (Dependency Layer): ").append(relativePath).append(" ---\n")
                        .append(processedContent).append("\n");

                hasValidFile = true;
                validCount++;
            } catch (IOException e) {
                log.warn("读取参考文件失败: {}", relativePath, e);
            }
        }

        if (hasValidFile) {
            chatMemory.add(UserMessage.from(contextBuilder.toString()));
            log.debug("已添加 {} 个参考文件到上下文", validCount);
        }

        return validCount;
    }

    private String pruneVueContent(String content) {
        // 匹配 <script> ... </script> 或 <script setup> ... </script>
        Pattern scriptPattern = Pattern.compile("<script.*?>[\\s\\S]*?</script>", Pattern.CASE_INSENSITIVE);
        Matcher matcher = scriptPattern.matcher(content);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            sb.append(matcher.group()).append("\n");
        }
        String result = sb.toString().trim();
        if (result.isEmpty()) {
            return "// [Vue Template/Style pruned, no script found]";
        }
        return result + "\n// [Vue Template/Style pruned for context efficiency]";
    }

    private String extractRouteSummary(Path routerFilePath) {
        try {
            String content = Files.readString(routerFilePath);
            List<String> routeLines = new ArrayList<>();

            // 简单正则匹配 path 和 component（适用于标准 Vue Router 写法）
            Pattern routePattern = Pattern.compile(
                    "path\\s*:\\s*['\"]([^'\"]+)['\"]\\s*,\\s*component\\s*:\\s*(\\w+)",
                    Pattern.CASE_INSENSITIVE
            );

            Matcher matcher = routePattern.matcher(content);
            while (matcher.find()) {
                String path = matcher.group(1);
                String component = matcher.group(2);
                routeLines.add("- " + path + " → " + component);
            }

            if (routeLines.isEmpty()) {
                return "// [No routes found in file]";
            }

            return "[Route Summary]\n" + String.join("\n", routeLines);
        } catch (Exception e) {
            log.warn("Failed to parse route file: {}", routerFilePath, e);
            return "// [Parse error: " + e.getMessage() + "]";
        }
    }

    @Override
    public ChatMessage buildUserInfo(Long appId, String message, ChatHistoryMessageTypeEnum messageType, boolean isUserVisible) {
        ChatMessage chatMessageInfo = new ChatMessage();
        chatMessageInfo.setAppId(appId);
        chatMessageInfo.setMessage(message);
        chatMessageInfo.setMessageType(messageType.getValue());
        chatMessageInfo.setUserVisible(isUserVisible ? 1 : 0);
        return chatMessageInfo;
    }

    @Override
    public ProjectSkeletonDTO getLastSkeletonByType(Long appId, Long userId, String value, GenerationContext context) {
        ChatHistory chatHistory = chatHistoryMapper.selectOneByQuery(new QueryWrapper().eq(ChatHistory::getAppId, appId).eq(ChatHistory::getUserId, userId).eq(ChatHistory::getMessageType, value));
        String message = chatHistory.getMessage();
        ProjectSkeletonDTO skeleton = null;
        try {
            skeleton = objectMapper.readValue(message, ProjectSkeletonDTO.class);
        } catch (JsonProcessingException e) {
            log.error("json序列化失败{}", e.getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        context.setSkeletonId(chatHistory.getId());
        return skeleton;
    }

    @Override
    public void updateSkeleton(ProjectSkeletonDTO skeleton, Long Id) {
        ChatHistory chatHistory = UpdateEntity.of(ChatHistory.class, Id);
        try {
            chatHistory.setMessage(objectMapper.writeValueAsString(skeleton));
            chatHistoryMapper.update(chatHistory);
        } catch (JsonProcessingException e) {
            log.error("json序列化失败{}", e.getMessage());
        }

    }

    @Override
    public void deleteSkeleton(Long appId, AiCodeGenStage aiCodeGenStage) {
        chatHistoryMapper.deleteByQuery(new QueryWrapper().eq(ChatHistory::getAppId, appId)
                .eq(ChatHistory::getMessageType, aiCodeGenStage.getValue()));
    }

    public List<ChatHistory> builderChatHistory(List<ChatMessage> chatMessages, Long userId) {
        List<ChatHistory> chatHistoryList = CollUtil.newArrayList();
        for (ChatMessage chatMessage : chatMessages) {
            ChatHistory chatHistory = ChatHistory.builder()
                    .appId(chatMessage.getAppId())
                    .message(chatMessage.getMessage())
                    .messageType(chatMessage.getMessageType())
                    .userId(userId)
                    .build();
            chatHistoryList.add(chatHistory);
        }
        return chatHistoryList;
    }


}
