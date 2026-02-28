package com.zriyo.service.workflow.action;

import lombok.Data;

import java.util.Map;

/**
 * 工作流执行上下文（最小实现）
 *
 * @author Zriyo AI
 * @since 2026-02-25
 */
@Data
public class ActionContext {

    private String taskId;
    private Long appId;
    private String workflowId;
    private String stage;
    private String action;
    private String traceId;
    private Map<String, Object> payload;
}
