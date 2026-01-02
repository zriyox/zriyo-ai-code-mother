package com.zriyo.aicodemother.model.dto.app;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
public class AppDeployRequest implements Serializable {

    /**
     * 应用 id
     */
    private Long appId;

    /**
     * 应用部署名称
     */
    @NotBlank(message = "应用部署名称不能为空")
    private String deployName;

    private static final long serialVersionUID = 1L;
}
