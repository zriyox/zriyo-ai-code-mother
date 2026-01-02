package com.zriyo.aicodemother.model.dto.app;

import com.zriyo.aicodemother.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
public class AppQueryRequest extends PageRequest implements Serializable {


    /**
     * 应用名称
     */
    private String appName;


    /**
     * 优先级
     */
    private Integer priority;

    /**
     * 是否发布
     */
    private Integer isPublished;


    @Serial
    private static final long serialVersionUID = 1L;
}
