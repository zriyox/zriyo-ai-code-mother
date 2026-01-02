package com.zriyo.aicodemother.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 运行时错误反馈详情（由前端上报）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuntimeFeedbackDTO {
    
    // 错误消息（控制台日志或用户描述）
    private String errorMsg;
    
    // 疑似出问题的文件路径（如 src/pages/Home.vue）
    private String errorFile;
    
    // 错误堆栈或上下文（JSON 字符串）
    private String context;

    // 是否是用户手动描述的逻辑问题
    private Boolean isManual;
}
