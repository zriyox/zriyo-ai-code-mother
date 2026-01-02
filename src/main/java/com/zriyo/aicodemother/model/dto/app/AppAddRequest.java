package com.zriyo.aicodemother.model.dto.app;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 应用创建请求
 */
@Data
public class AppAddRequest implements Serializable {

    /**
     * 应用初始化的 prompt
     */
    @NotBlank(message = "初始化提示不能为空")
    private String initPrompt;

    private String appName;


    private static final long serialVersionUID = 1L;
}
