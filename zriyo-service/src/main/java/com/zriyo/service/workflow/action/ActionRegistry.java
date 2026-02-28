package com.zriyo.service.workflow.action;

import com.zriyo.common.exception.BusinessException;
import com.zriyo.common.result.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;

/**
 * Action 注册表（策略分发）
 *
 * 约定：Spring Bean 名称即 action 名称，例如：
 * - @Component("generate_file")
 * - @Component("run_check")
 *
 * @author Zriyo AI
 * @since 2026-02-25
 */
@Component
public class ActionRegistry {

    private final Map<String, ActionHandler> handlerMap;

    public ActionRegistry(Map<String, ActionHandler> handlerMap) {
        this.handlerMap = handlerMap;
    }

    public ActionHandler get(String action) {
        ActionHandler handler = handlerMap.get(action);
        if (handler == null) {
            throw new BusinessException(ErrorCode.AGENT_NOT_FOUND, "ActionHandler not found: " + action);
        }
        return handler;
    }

    public void assertAllowed(String action, Collection<String> allowedActions) {
        if (allowedActions == null || allowedActions.isEmpty()) {
            return;
        }
        if (!allowedActions.contains(action)) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED, "Action not allowed: " + action);
        }
    }
}
