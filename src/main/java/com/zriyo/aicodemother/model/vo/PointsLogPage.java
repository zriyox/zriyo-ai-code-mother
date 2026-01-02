package com.zriyo.aicodemother.model.vo;

import lombok.Data;

import java.time.LocalDateTime;
@Data
public class PointsLogPage {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 本次积分变动值（正数为增加，负数为扣除）
     */
    private Integer changeAmount;

    /**
     * 本次变动后的可用积分余额
     */
    private Integer balanceAfter;

    /**
     * 变动原因编码，如：daily_sign（签到）、redeem_code（兑换码）、purchase（消费返积分）等
     */
    private String reason;

    /**
     * 关联业务ID，如订单号、签到记录ID、积分码ID等，便于追溯来源
     */
    private Long relatedId;

    /**
     * 记录创建时间
     */
    private LocalDateTime createdAt;
}
