package com.zriyo.common.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 积分变动原因枚举
 *
 * @author Zriyo AI
 * @since 2025-02-17
 */
@Getter
public enum PointsReasonEnum {

    // ========== 签到奖励 ==========
    /**
     * 每日签到
     */
    DAILY_SIGN("daily_sign", "每日签到", 100),

    // ========== 兑换相关 ==========
    /**
     * 积分码兑换
     */
    REDEEM_CODE("redeem_code", "积分码兑换", 5000),

    // ========== AI 消费（新架构） ==========
    /**
     * Agent 对话消费 (每次对话)
     */
    AGENT_CHAT("agent_chat", "Agent 对话消费", -10),

    /**
     * 代码生成消费
     */
    CODE_GENERATE("code_generate", "代码生成", -50),

    /**
     * 代码修复消费
     */
    CODE_FIX("code_fix", "代码修复", -30),

    /**
     * 文档分析消费
     */
    DOC_ANALYZE("doc_analyze", "文档分析", -20),

    /**
     * 图表生成消费
     */
    CHART_GENERATE("chart_generate", "图表生成", -15),

    /**
     * Token 超额消费 (按 Token 计费)
     */
    TOKEN_OVERAGE("token_overage", "Token 超额", -1),

    // ========== 应用相关 ==========
    /**
     * 应用部署消费
     */
    APP_DEPLOY("app_deploy", "应用部署", -100),

    /**
     * 应用生成消费 (旧版兼容)
     */
    APP_GENERATE("app_generate", "生成应用", -50),

    // ========== 系统相关 ==========
    /**
     * 系统补偿
     */
    SYSTEM_COMPENSATION("system_compensation", "系统补偿", 10),

    /**
     * 邀请奖励
     */
    INVITE_REWARD("invite_reward", "邀请奖励", 1000),

    /**
     * 任务完成奖励
     */
    TASK_REWARD("task_reward", "任务完成", 50);

    private static final Map<String, PointsReasonEnum> CODE_TO_ENUM_MAP =
            Arrays.stream(values())
                    .collect(Collectors.toMap(PointsReasonEnum::getCode, Function.identity()));

    private final String code;
    private final String description;
    private final Integer points;

    PointsReasonEnum(String code, String description, Integer points) {
        this.code = code;
        this.description = description;
        this.points = points;
    }

    /**
     * 根据 code 获取枚举
     */
    public static PointsReasonEnum fromCode(String code) {
        return CODE_TO_ENUM_MAP.get(code);
    }

    /**
     * 是否有固定积分值
     */
    public boolean hasFixedPoints() {
        return this.points != null;
    }

    /**
     * 获取固定积分值
     */
    public int getFixedPoints() {
        if (this.points == null) {
            throw new IllegalStateException("积分值不固定: " + this.code);
        }
        return this.points;
    }

    /**
     * 获取积分值或默认值
     */
    public int getPointsOrDefault(int fallback) {
        return this.points != null ? this.points : fallback;
    }
}
