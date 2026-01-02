package com.zriyo.aicodemother.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * App 数量统计 VO
 */
@Data
public class AppCountVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 当前已安装的 App 数量
     */
    private Integer currentCount;

    /**
     * 系统允许的最大 App 安装数量（例如：1000）
     */
    private Integer maxCount;


}
