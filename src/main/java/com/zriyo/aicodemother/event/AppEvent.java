package com.zriyo.aicodemother.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class AppEvent extends ApplicationEvent {
    private final Long appId;
    private final String OssUrl;

    public AppEvent(Object source, Long appId, String ossUrl) {
        super(source);
        this.appId = appId;
        OssUrl = ossUrl;
    }
}
