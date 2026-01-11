package com.zriyo.common.constant.message;

import lombok.Getter;

/**
 * 流式消息类型枚举
 * 用于 Server-Sent Events (SSE) 的 event 字段或 data 中的 type 字段
 */
@Getter
public enum StreamMessageTypeEnum {

    // --- 基础 AI 交互 ---
    AI_RESPONSE("ai_response", "AI响应内容"), // 普通的对话文本
    AI_DONE("done", "[DONE]"), // 结束标志
    //用户手动取消
    CANCEL("cancel", "用户手动取消"),
    ERROR("error", "通用错误"),

    // --- 普通工具交互 ---
    TOOL_REQUEST("tool_request", "工具调用请求"),
    TOOL_PROCESS("tool_process", "工具执行过程日志"), // 例如：正在搜索、正在计算...
    TOOL_EXECUTED("tool_executed", "工具执行结果"),
    TOOL_ERROR("tool_error", "工具执行异常"),
    TOOL_DONE("tool_done", "工具调用结束"),

    // --- 代码生成工具交互 ---
    CODE_TOOL_REQUEST("code_tool_request", "代码工具请求"),
    CODE_TOOL_PROCESS("code_tool_process", "代码工具执行过程"),
    CODE_TOOL_EXECUTED("code_tool_executed", "代码工具执行结果"),
    //ping 心跳
    PING("ping", "心跳"),

    // --- 🏥 ProjectDoctor 诊断与修复相关 (新增) ---
    /**
     * 诊断过程通知
     * 场景：发送 "正在挂载依赖..."、"正在启动浏览器..." 等进度条文案
     */
    DIAGNOSIS_PROCESS("diagnosis_process", "代码诊断进行中"),

    /**
     * 诊断发现缺陷 (需要 AI 介入修复)
     * 场景：静态检查不通过、运行时报错。Data 载体通常是 DiagnosisResult 对象。
     */
    DIAGNOSIS_ERROR("diagnosis_error", "发现代码缺陷"),

    /**
     * 诊断通过 (项目健康)
     * 场景：所有检查均通过，无报错。
     */
    DIAGNOSIS_SUCCESS("diagnosis_success", "代码诊断通过"),

    /**
     * 系统级异常
     * 场景：Playwright 启动失败、磁盘满、软链接权限不足等非代码逻辑错误。
     */
    SYSTEM_ERROR("system_error", "系统内部异常");


    private final String value;
    private final String text;

    StreamMessageTypeEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    /**
     * 根据值获取枚举
     */
    public static StreamMessageTypeEnum getEnumByValue(String value) {
        for (StreamMessageTypeEnum typeEnum : values()) {
            if (typeEnum.getValue().equals(value)) {
                return typeEnum;
            }
        }
        return null;
    }
}
