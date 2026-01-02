package com.zriyo.aicodemother.model.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 积分变动原因枚举
 * 对应 points_log.reason 字段
 *
 * 注意：
 * - REDEEM_CODE 的积分值由兑换码本身决定，此处不设固定值（points = null）
 * - 其他类型均为固定奖励/消耗值（正数表示奖励，负数表示消耗）
 */
@Getter
public enum PointsReasonEnum {

    // 每日签到：+5 分
    DAILY_SIGN("daily_sign", "每日签到", 100),

    // 兑换码：积分由码本身决定，此处不固定
    REDEEM_CODE("redeem_code", "积分码兑换", 5000),

    // 对话消费：每次对话 -10 分（假设按次扣费）
    CHAT_CONSUME("chat_consume", "对话消费", -1),

    // 生成应用：-20 分
    APP_GENERATE("app_generate", "生成应用", -20),

    // 系统补偿：通常由运营指定，但可设默认 +10（实际使用时可覆盖）
    SYSTEM_COMPENSATION("system_compensation", "系统补偿", 10);

    /**
     * 存入数据库的 code 值（与 reason 字段一致）
     */
    private final String code;

    /**
     * 中文描述（用于日志、前端展示）
     */
    private final String description;

    /**
     * 默认积分变动值（单位：分）
     * - 正数：增加积分
     * - 负数：扣除积分
     * - null：积分值不固定，需外部传入（如兑换码）
     */
    private final Integer points;

    PointsReasonEnum(String code, String description, Integer points) {
        this.code = code;
        this.description = description;
        this.points = points;
    }

    // ===== 静态工具方法 =====

    private static final Map<String, PointsReasonEnum> CODE_TO_ENUM_MAP =
            Arrays.stream(values())
                    .collect(Collectors.toMap(PointsReasonEnum::getCode, Function.identity()));

    /**
     * 根据 code 获取枚举（安全，避免 NPE）
     */
    public static PointsReasonEnum fromCode(String code) {
        return CODE_TO_ENUM_MAP.get(code);
    }

    /**
     * 判断该原因是否具有固定积分值
     */
    public boolean hasFixedPoints() {
        return this.points != null;
    }

    /**
     * 获取固定积分值（仅当 hasFixedPoints() 为 true 时调用）
     * @throws IllegalStateException 如果积分值不固定
     */
    public int getFixedPoints() {
        if (this.points == null) {
            throw new IllegalStateException("积分值不固定: " + this.code);
        }
        return this.points;
    }

    /**
     * 安全获取积分值（若不固定则返回 fallback）
     */
    public int getPointsOrDefault(int fallback) {
        return this.points != null ? this.points : fallback;
    }
}
