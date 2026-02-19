package com.zriyo.common.enums;

import lombok.Getter;

/**
 * 聊天历史消息类型枚举
 *
 * @author Zriyo AI
 * @since 2025-02-17
 */
@Getter
public enum ChatHistoryMessageTypeEnum {

    /**
     * 用户消息
     */
    USER("用户", "user"),

    /**
     * AI 回复
     */
    AI("AI", "ai"),

    /**
     * 系统消息
     */
    SYSTEM("系统", "system"),

    /**
     * Agent 思考过程
     */
    AGENT_THOUGHT("Agent 思考", "agent_thought"),

    /**
     * Tool 调用
     */
    TOOL_CALL("工具调用", "tool_call"),

    /**
     * Tool 结果
     */
    TOOL_RESULT("工具结果", "tool_result"),

    /**
     * 进度更新
     */
    PROGRESS("进度更新", "progress"),

    /**
     * 错误消息
     */
    ERROR("错误", "error"),

    /**
     * 警告消息
     */
    WARNING("警告", "warning"),

    /**
     * 文件操作
     */
    FILE_OPERATION("文件操作", "file_operation"),

    /**
     * 代码片段
     */
    CODE_CHUNK("代码片段", "code_chunk");

    private final String text;
    private final String value;

    ChatHistoryMessageTypeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     */
    public static ChatHistoryMessageTypeEnum getEnumByValue(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        for (ChatHistoryMessageTypeEnum anEnum : ChatHistoryMessageTypeEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }

    /**
     * 根据 code 获取枚举（兼容方法）
     */
    public static ChatHistoryMessageTypeEnum fromCode(String code) {
        return getEnumByValue(code);
    }
}
