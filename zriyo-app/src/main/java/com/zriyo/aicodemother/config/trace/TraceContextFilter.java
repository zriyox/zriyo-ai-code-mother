package com.zriyo.aicodemother.config.trace;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Trace 上下文过滤器。
 *
 * <p>职责：</p>
 * <ul>
 *     <li>读取/生成 traceId、requestId</li>
 *     <li>写入 MDC，供日志与下游 HTTP 透传</li>
 *     <li>回写响应头，便于前端与网关排障</li>
 * </ul>
 *
 * @author Zriyo AI
 * @since 2026-03-04
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class TraceContextFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String TASK_ID_HEADER = "X-Task-Id";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (!StringUtils.hasText(traceId)) {
            traceId = newId();
        }

        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (!StringUtils.hasText(requestId)) {
            requestId = newId();
        }

        String taskId = request.getHeader(TASK_ID_HEADER);

        MDC.put("traceId", traceId);
        MDC.put("requestId", requestId);
        if (StringUtils.hasText(taskId)) {
            MDC.put("taskId", taskId);
        }

        response.setHeader(TRACE_ID_HEADER, traceId);
        response.setHeader(REQUEST_ID_HEADER, requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("traceId");
            MDC.remove("requestId");
            MDC.remove("taskId");
        }
    }

    private String newId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
