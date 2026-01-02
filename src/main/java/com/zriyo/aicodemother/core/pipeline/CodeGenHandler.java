package com.zriyo.aicodemother.core.pipeline;

import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

// 抽象生成处理器

public abstract class CodeGenHandler {
    protected CodeGenHandler next;

    public void setNext(CodeGenHandler next) {
        this.next = next;
    }

    // 核心方法：处理生成请求
    public final Flux<ServerSentEvent<Object>> handle(GenerationContext context) {
        if (context.isTerminated()) {
            return Flux.empty();
        }
        return doHandle(context);
    }

    // 子类决定是否能处理此上下文（通常都返回 true）
    protected boolean canHandle(GenerationContext context) {
        return true;
    }

    // 提供一个方法给子类调用下一个 Handler
    protected void handleNext(GenerationContext context) {
        if (next != null && !context.isTerminated()) {
            next.handle(context);
        }
    }

    // 子类实现具体逻辑
    protected abstract Flux<ServerSentEvent<Object>> doHandle(GenerationContext context);




}
