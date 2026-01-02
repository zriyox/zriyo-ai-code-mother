package com.zriyo.aicodemother.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatHistoryQueryVo {
    /**
     * id
     */
    private Long id;
    /**
     * 消息
     */
    private String message;

    /**
     * user/ai
     */

    private String messageType;


    private LocalDateTime createTime;
}
