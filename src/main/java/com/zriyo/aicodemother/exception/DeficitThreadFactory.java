package com.zriyo.aicodemother.exception;


import lombok.AllArgsConstructor;

import java.util.concurrent.ThreadFactory;
@AllArgsConstructor
public class DeficitThreadFactory  implements ThreadFactory {
    private static final MyUncaughtExceptionHandler MY_UNCAUGHT_EXCEPTION_HANDLER = new MyUncaughtExceptionHandler();
    private ThreadFactory original;

    @Override
    public Thread newThread(Runnable r) {
        //调用了
        Thread thread = original.newThread(r);
        thread.setUncaughtExceptionHandler(MY_UNCAUGHT_EXCEPTION_HANDLER);
        return thread;
    }
}

