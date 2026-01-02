package com.zriyo.aicodemother.model.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户积分账户（逻辑外键 + 乐观锁） 实体类。
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("user_points")
public class UserPoints implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID，逻辑外键，关联 users.id
     */
    @Id
    private Long userId;

    /**
     * 累计获得总积分（不可减少）
     */
    private Integer totalPoints;

    /**
     * 当前可用积分（可用于兑换或扣减）
     */
    private Integer availablePoints;

    /**
     * 已使用/已消耗积分（total - available = used）
     */
    private Integer usedPoints;

    /**
     * 乐观锁版本号，用于防止并发更新覆盖
     */
    private Integer version;

    /**
     * 最后更新时间
     */
    private LocalDateTime updatedAt;

}
