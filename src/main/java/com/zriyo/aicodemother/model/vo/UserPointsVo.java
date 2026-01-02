package com.zriyo.aicodemother.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
@Data
public class UserPointsVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID，逻辑外键，关联 users.id
     */
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
     * 最后更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 是否签到
     */
    private Boolean isSign;
}
