package com.zriyo.common.sse.type;

/**
 * SSE 事件类型枚举
 * <p>
 * 基于 JoyAgent 架构的事件类型定义
 * </p>
 *
 * @author Zriyo AI
 * @since 2025-02-17
 */
public enum SseEventType {

    // ========== 请求生命周期 ==========
    /**
     * 请求开始
     */
    REQUEST_START("request_start", "请求开始"),

    /**
     * 请求完成
     */
    REQUEST_COMPLETE("request_complete", "请求完成"),

    /**
     * 请求取消
     */
    REQUEST_CANCEL("request_cancel", "请求取消"),

    // ========== Agent 编排 ==========
    /**
     * Agent 思考事件
     */
    AGENT_THOUGHT("agent_thought", "Agent 思考"),

    /**
     * Agent 切换事件
     */
    AGENT_SWITCH("agent_switch", "Agent 切换"),

    /**
     * Agent 开始执行
     */
    AGENT_START("agent_start", "Agent 开始"),

    /**
     * Agent 执行完成
     */
    AGENT_COMPLETE("agent_complete", "Agent 完成"),

    // ========== Tool 执行 ==========
    /**
     * Tool 调用事件
     */
    TOOL_CALL("tool_call", "Tool 调用"),

    /**
     * Tool 结果事件
     */
    TOOL_RESULT("tool_result", "Tool 结果"),

    /**
     * Tool 错误事件
     */
    TOOL_ERROR("tool_error", "Tool 错误"),

    // ========== 进度更新 ==========
    /**
     * 进度更新事件
     */
    PROGRESS("progress", "进度更新"),

    /**
     * 阶段更新事件
     */
    STAGE("stage", "阶段更新"),

    /**
     * 文件生成事件
     */
    FILE_CREATED("file_created", "文件生成"),

    /**
     * 文件更新事件
     */
    FILE_UPDATED("file_updated", "文件更新"),

    // ========== 内容流式输出 ==========
    /**
     * 文本片段事件（流式输出）
     */
    TEXT_CHUNK("text_chunk", "文本片段"),

    /**
     * 代码片段事件（流式输出）
     */
    CODE_CHUNK("code_chunk", "代码片段"),

    /**
     * Markdown 片段事件（流式输出）
     */
    MARKDOWN_CHUNK("markdown_chunk", "Markdown 片段"),

    // ========== 错误与警告 ==========
    /**
     * 错误事件
     */
    ERROR("error", "错误"),

    /**
     * 警告事件
     */
    WARNING("warning", "警告"),

    // ========== 系统事件 ==========
    /**
     * 心跳事件
     */
    HEARTBEAT("heartbeat", "心跳"),

    /**
     * 日志事件
     */
    LOG("log", "日志");

    private final String code;
    private final String description;

    SseEventType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据 code 获取事件类型
     */
    public static SseEventType fromCode(String code) {
        for (SseEventType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
