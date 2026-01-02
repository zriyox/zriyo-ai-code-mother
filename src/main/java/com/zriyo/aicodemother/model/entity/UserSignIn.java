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
import java.sql.Date;
import java.time.LocalDateTime;

/**
 * 用户签到历史记录（逻辑外键，支持连续签到） 实体类。
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("user_sign_in")
public class UserSignIn implements Serializable {

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
     * 签到日期（格式：YYYY-MM-DD）
     */
    private Date signDate;

    /**
     * 截至当日的连续签到天数（由应用层计算）
     */
    private Integer continuousDays;

    /**
     * 本次签到获得的积分数
     */
    private Integer rewardPoints;

    /**
     * 记录创建时间
     */
    private LocalDateTime createdAt;

}
