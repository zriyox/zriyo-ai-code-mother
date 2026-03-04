package com.zriyo.common.util;

/**
 * LLM 计费工具（积分口径）。
 *
 * <p>约定：</p>
 * <ul>
 *     <li>调用次数：仅统计外部请求次数</li>
 *     <li>积分扣费：仅对 LLM 调用按 token 计费</li>
 *     <li>工具调用：默认不扣分（可按业务开关外部收费工具）</li>
 * </ul>
 *
 * @author Zriyo AI
 * @since 2026-03-04
 */
public final class LlmBillingUtils {

    /**
     * 默认换算：每 1000 token 扣 1 积分。
     */
    public static final int DEFAULT_TOKENS_PER_POINT = 1000;

    private LlmBillingUtils() {
    }

    /**
     * 根据 token 计算积分变动（扣减为负数）。
     *
     * @param totalTokens    总 token 数
     * @param tokensPerPoint 每积分对应 token 数（最小为 1）
     * @return 积分变动值（扣减返回负值）
     */
    public static int calculateLlmPointsCost(int totalTokens, int tokensPerPoint) {
        if (totalTokens <= 0) {
            return 0;
        }
        int normalizedTokensPerPoint = Math.max(1, tokensPerPoint);
        int pointUnits = (totalTokens + normalizedTokensPerPoint - 1) / normalizedTokensPerPoint;
        return -pointUnits;
    }

    /**
     * 使用默认换算规则计算积分变动。
     */
    public static int calculateLlmPointsCost(int totalTokens) {
        return calculateLlmPointsCost(totalTokens, DEFAULT_TOKENS_PER_POINT);
    }

    /**
     * 工具调用是否参与扣费（默认不计费）。
     *
     * @param externalPaidApi 是否外部付费 API 工具
     * @return 是否计费
     */
    public static boolean isToolBillable(boolean externalPaidApi) {
        return externalPaidApi;
    }

    /**
     * 规范化外部调用次数（最小为 0）。
     */
    public static int normalizeCallCount(Integer callCount) {
        if (callCount == null || callCount < 0) {
            return 0;
        }
        return callCount;
    }
}
