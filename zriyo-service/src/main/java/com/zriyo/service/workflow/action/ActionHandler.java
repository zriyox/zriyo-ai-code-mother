package com.zriyo.service.workflow.action;

/**
 * Action 执行器接口（策略/命令）
 *
 * @author Zriyo AI
 * @since 2026-02-25
 */
public interface ActionHandler {

    void execute(ActionContext context);
}
