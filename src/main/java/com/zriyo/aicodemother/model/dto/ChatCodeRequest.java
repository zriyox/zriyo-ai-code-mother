package com.zriyo.aicodemother.model.dto;

import lombok.Data;

@Data
public class ChatCodeRequest {

    private Long appId;
    private String message;
    
    // 运行时反馈（可选，由前端“修复”按钮触发时携带）
    private RuntimeFeedbackDTO feedback;
}
