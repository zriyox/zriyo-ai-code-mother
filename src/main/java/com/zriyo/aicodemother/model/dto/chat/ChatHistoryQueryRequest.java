package com.zriyo.aicodemother.model.dto.chat;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
public class ChatHistoryQueryRequest implements Serializable {
    /**
     * id
     */
    private Long id;

    /**
     * 应用id
     */
    @NotNull(message = "应用id不能为空")
    private Long appId;


    private static final long serialVersionUID = 1L;
}
