package com.zriyo.aicodemother.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
@Data
public class AppWithLatestVersionVO implements Serializable {
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
     * 应用初始化的 prompt
     */
    private String initPrompt;

    /**
     * 代码生成类型（枚举）
     */
    private String codeGenType;

    /**
     * 部署标识
     */
    private String deployKey;

    /**
     * 部署时间
     */
    private LocalDateTime deployedTime;


    /**
     * 是否最新版本
     */
    private Boolean isLatestVersion;

    /**
     * 当前版本
     */
    private String latestVersion;


    /**
     * 创建时间
     */
    private LocalDateTime createTime;



    private static final long serialVersionUID = 1L;
}
