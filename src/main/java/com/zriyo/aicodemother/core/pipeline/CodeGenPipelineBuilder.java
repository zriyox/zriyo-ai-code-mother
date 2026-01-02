package com.zriyo.aicodemother.core.pipeline;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class CodeGenPipelineBuilder {

    private final List<CodeGenHandler> handlers;

    // 自动注入所有 CodeGenHandler 并按 @Order 排序
    public CodeGenPipelineBuilder(List<CodeGenHandler> handlers) {
        handlers.forEach(h -> log.info("Handler in chain: {}", h.getClass().getSimpleName()));
        this.handlers = handlers.stream()
            .sorted(AnnotationAwareOrderComparator.INSTANCE)
            .collect(Collectors.toList());
    }

    public CodeGenHandler buildChain() {
        if (handlers.isEmpty()) return null;

        CodeGenHandler head = handlers.getFirst();
        CodeGenHandler current = head;

        for (int i = 1; i < handlers.size(); i++) {
            current.setNext(handlers.get(i));
            current = handlers.get(i);
        }
        return head;
    }
}
