package com.zriyo.aicodemother.config;

import com.zriyo.aicodemother.exception.DeficitThreadFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {
    /**
     * 异步线程池
     */
    public static final String AI_ASYNC_EXECUTOR = "AiAsyncExecutor";

    @Bean(name = AsyncConfig.AI_ASYNC_EXECUTOR)
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数
        executor.setCorePoolSize(5);
        // 最大线程数
        executor.setMaxPoolSize(10);
        // 队列容量
        executor.setQueueCapacity(25);
        // 线程存活时间（秒）
        executor.setKeepAliveSeconds(60);
        // 线程名称前缀
        executor.setThreadNamePrefix(AsyncConfig.AI_ASYNC_EXECUTOR + "-Async-Thread-");
        // 拒绝策略
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        //添加了一个异常捕获器
        executor.setThreadFactory(new DeficitThreadFactory(executor));
        // 初始化
        executor.initialize();
        return executor;
    }


}
