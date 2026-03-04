package com.zriyo.common.enums;

/**
 * 计费结算状态（对应 llm_call_log / llm_billing_ledger）。
 *
 * @author Zriyo AI
 * @since 2026-03-04
 */
public enum BillingSettleStatus {

    PENDING("PENDING", "待结算"),
    SETTLED("SETTLED", "已结算"),
    REVERSED("REVERSED", "已冲正");

    private final String code;
    private final String description;

    BillingSettleStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static BillingSettleStatus fromCode(String code) {
        if (code == null || code.isBlank()) {
            return PENDING;
        }
        String normalized = code.trim().toUpperCase();
        for (BillingSettleStatus status : values()) {
            if (status.code.equals(normalized)) {
                return status;
            }
        }
        return PENDING;
    }
}
