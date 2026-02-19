package com.zriyo.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 生产级虚拟线程（Virtual Threads）工具类
 * <p>
 * 提供安全、高效、可监控的虚拟线程执行方法。
 * 适用于 I/O 密集型任务，如 HTTP 调用、数据库操作、文件读写等。
 * </p>
 *
 * <h2>核心特性：</h2>
 * <ul>
 *   <li>MDC 上下文传播：自动传递日志上下文</li>
 *   <li>超时控制：支持任务级别的超时设置</li>
 *   <li>异常处理：统一的异常处理和日志记录</li>
 *   <li>可观测性：内置执行耗时统计</li>
 *   <li>批量执行：支持并发执行多个任务</li>
 * </ul>
 *
 * <h2>使用示例：</h2>
 * <pre>{@code
 * // 简单异步执行
 * VirtualThreadUtils.executeAsync(() -> {
 *     // I/O 操作
 *     return fetchData();
 * });
 *
 * // 批量并发执行
 * List<String> results = VirtualThreadUtils.executeAll(List.of(
 *     () -> task1(),
 *     () -> task2(),
 *     () -> task3()
 * ));
 *
 * // 带超时的执行
 * String result = VirtualThreadUtils.executeWithTimeout(
 *     () -> slowTask(),
 *     Duration.ofSeconds(5)
 * );
 *
 * // Fire-and-Forget
 * VirtualThreadUtils.executeAndForget(() -> {
 *     sendNotification();
 * }, "notification");
 * }</pre>
 *
 * @author Zriyo AI
 * @since 2025-02-17
 */
public final class VirtualThreadUtils {

    private static final Logger log = LoggerFactory.getLogger(VirtualThreadUtils.class);

    /**
     * 默认超时时间：30秒
     */
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    /**
     * 私有构造函数，防止实例化
     */
    private VirtualThreadUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    // ==================== 基础执行器 ====================

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
     * 创建一个命名的虚拟线程工厂。
     *
     * @param prefix 线程名前缀
     * @return 线程工厂
     */
    public static ThreadFactory namedThreadFactory(String prefix) {
        return Thread.ofVirtual()
                .name(prefix, 0)
                .uncaughtExceptionHandler((t, e) -> {
                    log.error("[VirtualThread-{}] Uncaught exception", t.getName(), e);
                })
                .factory();
    }

    /**
     * 创建一个带命名的虚拟线程执行器。
     *
     * @param prefix 线程名前缀
     * @return 虚拟线程执行器
     */
    public static ExecutorService newNamedVirtualThreadExecutor(String prefix) {
        return Executors.newThreadPerTaskExecutor(namedThreadFactory(prefix));
    }

    // ==================== 简单异步执行 ====================

    /**
     * 异步执行无返回值的任务。
     *
     * @param task     要执行的任务
     * @param taskName 任务名称（用于日志）
     * @return 表示任务的 Future 对象
     */
    public static Future<?> executeAsync(Runnable task, String taskName) {
        ExecutorService executor = newNamedVirtualThreadExecutor("async-" + taskName);
        return executor.submit(wrapRunnable(task, taskName));
    }

    /**
     * 异步执行无返回值的任务。
     *
     * @param task 要执行的任务
     * @return 表示任务的 Future 对象
     */
    public static Future<?> executeAsync(Runnable task) {
        return executeAsync(task, "unnamed");
    }

    /**
     * 异步执行有返回值的任务。
     *
     * @param supplier 任务提供者
     * @param taskName 任务名称
     * @param <T>      返回值类型
     * @return CompletableFuture
     */
    public static <T> CompletableFuture<T> executeAsync(Supplier<T> supplier, String taskName) {
        ExecutorService executor = newNamedVirtualThreadExecutor("async-" + taskName);
        return CompletableFuture.supplyAsync(
                wrapSupplier(supplier, taskName),
                executor
        );
    }

    /**
     * 异步执行有返回值的任务。
     *
     * @param supplier 任务提供者
     * @param <T>      返回值类型
     * @return CompletableFuture
     */
    public static <T> CompletableFuture<T> executeAsync(Supplier<T> supplier) {
        return executeAsync(supplier, "unnamed");
    }

    // ==================== 带超时的执行 ====================

    /**
     * 带超时地执行任务。
     *
     * @param supplier 任务提供者
     * @param timeout  超时时间
     * @param taskName 任务名称
     * @param <T>      返回值类型
     * @return 任务结果
     * @throws TimeoutException     如果超时
     * @throws ExecutionException   如果执行失败
     * @throws InterruptedException 如果中断
     */
    public static <T> T executeWithTimeout(Supplier<T> supplier, Duration timeout, String taskName)
            throws TimeoutException, ExecutionException, InterruptedException {

        ExecutorService executor = newNamedVirtualThreadExecutor("timeout-" + taskName);
        Supplier<T> wrapped = wrapSupplier(supplier, taskName);
        Future<T> future = executor.submit(wrapped::get);

        try {
            return future.get(timeout.toSeconds(), TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            log.warn("[VirtualThread] Task '{}' timed out after {}", taskName, timeout);
            throw e;
        } finally {
            shutdownAndAwaitTermination(executor, Duration.ofSeconds(5));
        }
    }

    /**
     * 带超时地执行任务（使用默认超时30秒）。
     *
     * @param supplier 任务提供者
     * @param <T>      返回值类型
     * @return 任务结果
     */
    public static <T> T executeWithTimeout(Supplier<T> supplier) throws Exception {
        return executeWithTimeout(supplier, DEFAULT_TIMEOUT, "unnamed");
    }

    // ==================== 批量执行 ====================

    /**
     * 批量执行任务并等待所有完成。
     *
     * @param tasks 任务列表
     * @param <T>   返回值类型
     * @return 结果列表
     * @throws ExecutionException   如果任意任务失败
     * @throws InterruptedException 如果中断
     */
    @SafeVarargs
    public static <T> List<T> executeAll(Supplier<T>... tasks)
            throws ExecutionException, InterruptedException {

        return executeAll(DEFAULT_TIMEOUT, List.of(tasks));
    }

    /**
     * 批量执行任务并等待所有完成。
     *
     * @param tasks 任务列表
     * @param <T>   返回值类型
     * @return 结果列表
     * @throws ExecutionException   如果任意任务失败
     * @throws InterruptedException 如果中断
     */
    public static <T> List<T> executeAll(List<Supplier<T>> tasks)
            throws ExecutionException, InterruptedException {

        return executeAll(DEFAULT_TIMEOUT, tasks);
    }

    /**
     * 批量执行任务并等待所有完成（带超时）。
     *
     * @param timeout 全局超时时间
     * @param tasks   任务列表
     * @param <T>     返回值类型
     * @return 结果列表
     * @throws ExecutionException   如果任意任务失败
     * @throws InterruptedException 如果中断
     */
    public static <T> List<T> executeAll(Duration timeout, List<Supplier<T>> tasks)
            throws ExecutionException, InterruptedException {

        if (tasks.isEmpty()) {
            return List.of();
        }

        Instant start = Instant.now();
        ExecutorService executor = newNamedVirtualThreadExecutor("batch");

        try {
            List<Future<T>> futures = new ArrayList<>();
            for (int i = 0; i < tasks.size(); i++) {
                Supplier<T> task = tasks.get(i);
                String taskName = "batch-" + i;
                Supplier<T> wrapped = wrapSupplier(task, taskName);
                futures.add(executor.submit(wrapped::get));
            }

            List<T> results = new ArrayList<>(tasks.size());
            for (int i = 0; i < futures.size(); i++) {
                Future<T> future = futures.get(i);

                // 计算剩余超时时间
                Duration remaining = timeout.minus(Duration.between(start, Instant.now()));
                if (remaining.isNegative() || remaining.isZero()) {
                    future.cancel(true);
                    throw new TimeoutException("Batch task " + i + " timed out");
                }

                results.add(future.get(remaining.toSeconds(), TimeUnit.SECONDS));
            }

            log.debug("[VirtualThread] Batch completed: {} tasks in {}",
                    tasks.size(), Duration.between(start, Instant.now()));

            return results;

        } catch (TimeoutException e) {
            log.error("[VirtualThread] Batch execution timed out");
            throw new ExecutionException(e);
        } finally {
            shutdownAndAwaitTermination(executor, Duration.ofSeconds(5));
        }
    }

    /**
     * 批量执行任务，任意一个成功即返回。
     *
     * @param tasks 任务列表
     * @param <T>   返回值类型
     * @return 第一个成功的结果
     * @throws InterruptedException 如果中断
     * @throws ExecutionException   如果所有任务都失败
     */
    @SafeVarargs
    public static <T> T executeAny(Supplier<T>... tasks)
            throws InterruptedException, ExecutionException {

        return executeAny(DEFAULT_TIMEOUT, List.of(tasks));
    }

    /**
     * 批量执行任务，任意一个成功即返回（带超时）。
     *
     * @param timeout 超时时间
     * @param tasks   任务列表
     * @param <T>     返回值类型
     * @return 第一个成功的结果
     * @throws InterruptedException 如果中断
     * @throws ExecutionException   如果所有任务都失败
     */
    public static <T> T executeAny(Duration timeout, List<Supplier<T>> tasks)
            throws InterruptedException, ExecutionException {

        if (tasks.isEmpty()) {
            throw new IllegalArgumentException("Tasks list cannot be empty");
        }

        ExecutorService executor = newNamedVirtualThreadExecutor("any");
        CompletionService<T> completionService = new ExecutorCompletionService<>(executor);

        try {
            List<Future<T>> futures = new ArrayList<>();
            for (int i = 0; i < tasks.size(); i++) {
                Supplier<T> task = tasks.get(i);
                String taskName = "any-" + i;
                Supplier<T> wrapped = wrapSupplier(task, taskName);
                futures.add(completionService.submit(wrapped::get));
            }

            Future<T> result = completionService.poll(timeout.toSeconds(), TimeUnit.SECONDS);
            if (result == null) {
                // 超时，取消所有任务
                for (Future<?> f : futures) {
                    f.cancel(true);
                }
                throw new TimeoutException("No task completed in time");
            }

            // 取消剩余任务
            for (Future<?> f : futures) {
                f.cancel(true);
            }

            return result.get();

        } catch (TimeoutException e) {
            throw new ExecutionException(e);
        } finally {
            shutdownAndAwaitTermination(executor, Duration.ofSeconds(1));
        }
    }

    // ==================== Fire-and-Forget ====================

    /**
     * 执行任务并忽略结果，异常仅记录日志。
     * <p>
     * 适用于不需要等待结果的场景，如异步发送通知、记录日志等。
     * </p>
     *
     * @param task     要执行的任务
     * @param taskName 任务名称
     */
    public static void executeAndForget(Runnable task, String taskName) {
        CompletableFuture.runAsync(
                wrapRunnable(task, taskName),
                newVirtualThreadExecutor()
        ).exceptionally(throwable -> {
            log.error("[VirtualThread] Task '{}' failed", taskName, throwable);
            return null;
        });
    }

    /**
     * 执行任务并忽略结果，异常仅记录日志。
     *
     * @param task 要执行的任务
     */
    public static void executeAndForget(Runnable task) {
        executeAndForget(task, "unnamed");
    }

    /**
     * 执行任务并忽略结果，支持自定义异常处理。
     *
     * @param task             要执行的任务
     * @param exceptionHandler 异常处理器
     */
    public static void executeAndForget(Runnable task, Consumer<Throwable> exceptionHandler) {
        CompletableFuture.runAsync(
                wrapRunnable(task, "unnamed"),
                newVirtualThreadExecutor()
        ).exceptionally(throwable -> {
            exceptionHandler.accept(throwable);
            return null;
        });
    }

    // ==================== 延迟执行 ====================

    /**
     * 延迟执行任务。
     *
     * @param task     要执行的任务
     * @param delay    延迟时间
     * @param taskName 任务名称
     * @return ScheduledFuture
     */
    public static ScheduledFuture<?> schedule(Runnable task, Duration delay, String taskName) {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
                namedThreadFactory("schedule-" + taskName)
        );
        return scheduler.schedule(
                wrapRunnable(() -> {
                    task.run();
                    scheduler.shutdown();
                }, taskName),
                delay.toMillis(),
                TimeUnit.MILLISECONDS
        );
    }

    // ==================== 包装器 ====================

    /**
     * 包装 Runnable，添加 MDC 上下文传播和异常处理。
     */
    private static Runnable wrapRunnable(Runnable task, String taskName) {
        // 捕获当前线程的 MDC 上下文
        var contextMap = MDC.getCopyOfContextMap();

        return () -> {
            // 设置 MDC 上下文
            if (contextMap != null) {
                MDC.setContextMap(contextMap);
            }

            Instant start = Instant.now();
            String threadName = Thread.currentThread().getName();

            try {
                log.trace("[VirtualThread] Task '{}' started on thread {}", taskName, threadName);
                task.run();
                log.trace("[VirtualThread] Task '{}' completed in {}",
                        taskName, Duration.between(start, Instant.now()));

            } catch (Throwable t) {
                log.error("[VirtualThread] Task '{}' failed after {}",
                        taskName, Duration.between(start, Instant.now()), t);
                throw t;

            } finally {
                MDC.clear();
            }
        };
    }

    /**
     * 包装 Supplier，添加 MDC 上下文传播和异常处理。
     */
    private static <T> Supplier<T> wrapSupplier(Supplier<T> supplier, String taskName) {
        // 捕获当前线程的 MDC 上下文
        var contextMap = MDC.getCopyOfContextMap();

        return () -> {
            // 设置 MDC 上下文
            if (contextMap != null) {
                MDC.setContextMap(contextMap);
            }

            Instant start = Instant.now();
            String threadName = Thread.currentThread().getName();

            try {
                log.trace("[VirtualThread] Task '{}' started on thread {}", taskName, threadName);
                T result = supplier.get();
                log.trace("[VirtualThread] Task '{}' completed in {}",
                        taskName, Duration.between(start, Instant.now()));
                return result;

            } catch (Throwable t) {
                log.error("[VirtualThread] Task '{}' failed after {}",
                        taskName, Duration.between(start, Instant.now()), t);
                throw t;

            } finally {
                MDC.clear();
            }
        };
    }

    /**
     * 优雅地关闭执行器并等待任务完成。
     *
     * @param executor      要关闭的执行器
     * @param awaitTimeout  等待超时时间
     */
    private static void shutdownAndAwaitTermination(ExecutorService executor, Duration awaitTimeout) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(awaitTimeout.toSeconds(), TimeUnit.SECONDS)) {
                log.warn("[VirtualThread] Executor did not terminate in {}, forcing shutdown", awaitTimeout);
                executor.shutdownNow();
                if (!executor.awaitTermination(awaitTimeout.toSeconds(), TimeUnit.SECONDS)) {
                    log.error("[VirtualThread] Executor did not terminate after forced shutdown");
                }
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
