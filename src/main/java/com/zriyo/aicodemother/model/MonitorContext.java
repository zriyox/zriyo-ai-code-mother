package com.zriyo.aicodemother.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonitorContext implements Serializable {

    private String userId;

    private String appId;

    /**
     * 追踪任务交互步数（LLM与工具往返次数）
     * 使用 Atomic 确保在流式回调中的并发安全性
     */
    @Builder.Default
    private final AtomicInteger stepCount = new AtomicInteger(0);

    /**
     * 记录上一次动作的指纹（如：文件路径+总结内容）
     * 用于在工具层判断模型是否在原地踏步
     */
    @Builder.Default
    private final AtomicReference<String> lastActionFingerprint = new AtomicReference<>("");

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 步数自增并返回
     */
    public int incrementAndGetStep() {
        return stepCount.incrementAndGet();
    }

    /**
     * 逻辑查重：如果当前操作指纹与上一次完全一致，则视为重复
     */
    public boolean isRepeated(String currentFingerprint) {
        if (currentFingerprint == null || currentFingerprint.isEmpty()) return false;
        // getAndSet 返回旧值并设置新值
        String last = lastActionFingerprint.getAndSet(currentFingerprint);
        return currentFingerprint.equals(last);
    }
}
