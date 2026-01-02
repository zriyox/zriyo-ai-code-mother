package com.zriyo.aicodemother.model.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 积分变动流水日志（逻辑外键，全量可追溯） 实体类。
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("points_log")
public class PointsLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 用户ID，逻辑外键，关联 users.id
     */
    private Long userId;

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
