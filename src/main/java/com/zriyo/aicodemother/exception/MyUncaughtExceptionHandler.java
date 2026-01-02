package com.zriyo.aicodemother.exception;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MyUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        log.error("线程 [{}] 中发生未捕获异常：{}", t.getName(), e.getMessage(), e);
    }
}
