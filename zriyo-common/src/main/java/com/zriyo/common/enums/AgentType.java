package com.zriyo.common.enums;

/**
 * Agent 类型枚举
 * <p>
 * 基于 JoyAgent 架构的 Agent 类型定义
 * </p>
 *
 * @author Zriyo AI
 * @since 2025-02-17
 */
public enum AgentType {

    /**
     * 编排 Agent - 负责任务分解和规划
     */
    ORCHESTRATOR("orchestrator", "编排 Agent"),

    /**
     * 代码 Agent - 负责代码生成、修复、重构
     */
    CODE("code", "代码 Agent"),

    /**
     * 文档 Agent - 负责文档分析和处理
     */
    DOC("doc", "文档 Agent"),

    /**
     * 图表 Agent - 负责图表绘制和数据分析
     */
    CHART("chart", "图表 Agent"),

    /**
     * 部署 Agent - 负责应用部署和回滚
     */
    DEPLOY("deploy", "部署 Agent"),

    /**
     * 调试 Agent - 负责问题诊断和修复
     */
    DEBUG("debug", "调试 Agent"),

    /**
     * 测试 Agent - 负责测试用例生成和执行
     */
    TEST("test", "测试 Agent"),

    /**
     * 解释 Agent - 负责代码解释和文档生成
     */
    EXPLAIN("explain", "解释 Agent"),

    /**
     * 搜索 Agent - 负责代码搜索和定位
     */
    SEARCH("search", "搜索 Agent"),

    /**
     * 通用 Agent
     */
    GENERAL("general", "通用 Agent");

    private final String code;
    private final String description;

    AgentType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static AgentType fromCode(String code) {
        for (AgentType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return GENERAL;
    }
}
