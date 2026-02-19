package com.zriyo.common.enums;

/**
 * 意图类型枚举
 * <p>
 * 基于 JoyAgent 架构的意图识别类型
 * </p>
 *
 * @author Zriyo AI
 * @since 2025-02-17
 */
public enum IntentType {

    // ========== 代码相关 ==========
    /**
     * 代码生成
     */
    CODE_GEN("code_gen", "代码生成"),

    /**
     * 代码修复
     */
    CODE_FIX("code_fix", "代码修复"),

    /**
     * 代码解释
     */
    CODE_EXPLAIN("code_explain", "代码解释"),

    /**
     * 代码重构
     */
    CODE_REFACTOR("code_refactor", "代码重构"),

    /**
     * 代码审查
     */
    CODE_REVIEW("code_review", "代码审查"),

    // ========== 文档相关 ==========
    /**
     * 文档分析
     */
    DOC_ANALYZE("doc_analyze", "文档分析"),

    /**
     * 文档摘要
     */
    DOC_SUMMARIZE("doc_summarize", "文档摘要"),

    /**
     * 信息提取
     */
    DOC_EXTRACT("doc_extract", "信息提取"),

    // ========== 图表相关 ==========
    /**
     * 图表绘制
     */
    CHART_DRAW("chart_draw", "图表绘制"),

    /**
     * 数据分析
     */
    DATA_ANALYZE("data_analyze", "数据分析"),

    // ========== 部署相关 ==========
    /**
     * 部署
     */
    DEPLOY("deploy", "部署"),

    /**
     * 回滚
     */
    ROLLBACK("rollback", "回滚"),

    // ========== 调试相关 ==========
    /**
     * 调试
     */
    DEBUG("debug", "调试"),

    /**
     * 测试
     */
    TEST("test", "测试"),

    // ========== 其他 ==========
    /**
     * 未知意图
     */
    UNKNOWN("unknown", "未知意图");

    private final String code;
    private final String description;

    IntentType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static IntentType fromCode(String code) {
        for (IntentType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return UNKNOWN;
    }
}
