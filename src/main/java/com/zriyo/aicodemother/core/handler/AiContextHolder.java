package com.zriyo.aicodemother.core.handler;// 1. 引入 TTL

import com.alibaba.ttl.TransmittableThreadLocal;
import com.zriyo.aicodemother.model.MonitorContext;

public class AiContextHolder {
    private static final TransmittableThreadLocal<MonitorContext> CONTEXT = new TransmittableThreadLocal<>();

    public static void set(MonitorContext context) {
        CONTEXT.set(context);
    }

    public static MonitorContext get() {
        return CONTEXT.get();
    }

    public static void remove() {
        CONTEXT.remove();
    }
}
