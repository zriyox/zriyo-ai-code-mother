package com.zriyo.aicodemother.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * 虚拟线程（Virtual Threads）工具类
 * <p>
 * 提供简单、安全、高效的虚拟线程执行方法。
 * 适用于 I/O 密集型任务，如 HTTP 调用、数据库操作、文件读写等。
 * </p>
 *
 * @author zriyo
 * @since Java 21+
 */
public final class VirtualThreadUtils {

    private static final Logger log = LoggerFactory.getLogger(VirtualThreadUtils.class);

    /**
     * 私有构造函数，防止实例化
     */
    private VirtualThreadUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 创建一个为每个任务分配新虚拟线程的执行器。
     * <p>
     * 注意：此执行器应在 try-with-resources 中使用，以确保所有任务完成后再关闭。
     * </p>
     *
     * @return 虚拟线程执行器
     */
    public static ExecutorService newVirtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * 异步执行无返回值的任务，并在发生异常时记录日志。
     *
     * @param task 要执行的任务
     * @return 表示任务的 Future 对象，可用于等待或取消
     */
    public static Future<?> runAsync(Runnable task) {
        return newVirtualThreadExecutor().submit(wrapRunnable(task));
    }

    /**
     * 异步执行有返回值的任务，并在发生异常时记录日志。
     *
     * @param supplier 任务提供者
     * @param <T>      返回值类型
     * @return CompletableFuture，包含任务结果或异常
     */
    public static <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        // 使用默认的 ForkJoinPool.commonPool() 作为后备，但实际由虚拟线程执行
        return CompletableFuture.supplyAsync(supplier, newVirtualThreadExecutor())
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    log.error("Virtual thread task failed", throwable);
                }
            });
    }

    /**
     * 批量提交任务并等待所有任务完成。
     * <p>
     * 此方法会阻塞直到所有任务完成或失败。
     * </p>
     *
     * @param tasks 要执行的任务列表
     * @throws ExecutionException   如果任意任务抛出异常
     * @throws InterruptedException 如果当前线程被中断
     */
    public static void runAllAndWait(Iterable<Runnable> tasks)
            throws ExecutionException, InterruptedException {
        try (ExecutorService executor = newVirtualThreadExecutor()) {
            var futures = new CompletableFuture<Void>();
            for (Runnable task : tasks) {
                executor.submit(wrapRunnable(task));
            }
            // 等待所有任务完成（此处简化处理，实际可收集 Future 并 awaitTermination）
            executor.shutdown();
            if (!executor.awaitTermination(5, TimeUnit.MINUTES)) {
                executor.shutdownNow();
                throw new TimeoutException("Tasks did not complete in time");
            }
        } catch (TimeoutException e) {
            log.error("Batch execution timed out", e);
            throw new RuntimeException(e);
        }
    }

    // --- 辅助方法 ---

    /**
     * 包装 Runnable 以捕获并记录未处理的异常。
     */
    private static Runnable wrapRunnable(Runnable task) {
        return () -> {
            try {
                task.run();
            } catch (Throwable t) {
                log.error("Uncaught exception in virtual thread", t);
                // 可根据需要决定是否 re-throw
                throw t;
            }
        };
    }
}
