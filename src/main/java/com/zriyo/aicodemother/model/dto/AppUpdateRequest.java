package com.zriyo.aicodemother.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AppUpdateRequest {
    @NotNull(message = "appId cannot be null")
    private Long appId;
    @NotNull(message = "appName cannot be null")
    private String appName;
    private Boolean isOffline;

}
