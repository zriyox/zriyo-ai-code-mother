package com.zriyo.aicodemother.model.vo;

import lombok.Data;

import java.time.LocalDateTime;
@Data
public class AppInfoVO {
    /**
     * id
     */
    private Long id;

    /**
     * 应用名称
     */
    private String appName;

    /**
     * 应用封面
     */
    private String cover;

    /**
     * 部署标识
     */
    private String deployKey;

    /**
     * 部署时间
     */
    private LocalDateTime deployedTime;

    /**
     * 是否部署
     */
    private Boolean deployed;

    /**
     * 优先级
     */
    private Integer priority;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 是否部署
     */
    private int isPublished;

    private static final long serialVersionUID = 1L;
}
