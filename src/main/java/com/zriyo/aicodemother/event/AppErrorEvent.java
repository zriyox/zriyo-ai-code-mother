package com.zriyo.aicodemother.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter

public class AppErrorEvent extends ApplicationEvent {
    private final Long appId;
    public AppErrorEvent(Object source, Long appId) {
        super(source);
        this.appId = appId;
    }
}
