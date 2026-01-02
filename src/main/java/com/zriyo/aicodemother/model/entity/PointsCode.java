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
 * 积分兑换码管理表（逻辑外键，支持批量发放与追踪） 实体类。
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("points_code")
public class PointsCode implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 积分兑换码，全局唯一（如：XP7K9Q），由系统生成
     */
    private String code;

    /**
     * 该码可兑换的积分数（固定值）
     */
    private Integer points;

    /**
     * 使用状态：0=未使用，1=已使用，2=已过期
     */
    private Integer status;

    /**
     * 兑换用户ID，逻辑外键，关联 users.id；为空表示未兑换
     */
    private Long usedByUserId;

    /**
     * 实际兑换时间，NULL 表示未兑换
     */
    private LocalDateTime usedAt;

    /**
     * 码的过期时间，超过此时间不可再使用
     */
    private LocalDateTime expiredAt;

    /**
     * 创建者用户ID（通常为运营人员），逻辑外键，关联 users.id
     */
    private Long createdBy;

    /**
     * 码的创建时间
     */
    private LocalDateTime createdAt;

}
