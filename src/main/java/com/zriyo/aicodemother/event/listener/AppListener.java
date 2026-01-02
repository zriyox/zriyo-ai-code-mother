package com.zriyo.aicodemother.event.listener;

import com.zriyo.aicodemother.config.AsyncConfig;
import com.zriyo.aicodemother.event.AppErrorEvent;
import com.zriyo.aicodemother.event.AppEvent;
import com.zriyo.aicodemother.service.AppService;
import jodd.util.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class AppListener {
    private final AppService appService;

    @EventListener(classes = AppEvent.class)
    @Async(AsyncConfig.AI_ASYNC_EXECUTOR)
    public void updateAppOssUrl(AppEvent event) {
        if (StringUtil.isNotBlank(event.getOssUrl())) {
            appService.updateOssUrl(event.getAppId(), event.getOssUrl());
        }
    }
    @EventListener(classes = AppErrorEvent.class)
    @Async(AsyncConfig.AI_ASYNC_EXECUTOR)
    public void updateAppError(AppErrorEvent event) {
        Long appId = event.getAppId();
    }
}
