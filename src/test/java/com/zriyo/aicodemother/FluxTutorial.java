package com.zriyo.aicodemother;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Project Reactor Flux 核心用法详解
 * 适用于：流式响应（SSE）、异步数据处理、AI 生成流等场景
 */
public class FluxTutorial {

    //region 1. 创建 Flux：从不同源头生成数据流
    public static void createFlux() {
        System.out.println("=== 1. 创建 Flux ===");

        // 1.1 从固定值创建
        Flux<String> just = Flux.just("A", "B", "C");
        just.subscribe(System.out::println); // 输出: A B C

        // 1.2 从数组/集合创建
        Flux<Integer> fromArray = Flux.fromArray(new Integer[]{1, 2, 3});
        Flux<String> fromIterable = Flux.fromIterable(Arrays.asList("X", "Y", "Z"));

        // 1.3 空流 / 错误流 / 单值流
        Flux.empty();           // 无数据，直接 complete
        Flux.error(new RuntimeException("Oops!"));
        Flux.never();           // 永不结束（慎用）

        // 1.4 定时生成（常用于心跳、轮询）
        Flux<Long> interval = Flux.interval(Duration.ofSeconds(1))
                .take(3); // 只取前3个，避免无限流
        interval.subscribe(l -> System.out.println("Tick: " + l));

        // 1.5 手动创建流（最灵活！用于集成外部系统，如 LLM、文件读取）
        Flux<String> customFlux = Flux.create(sink -> {
            for (int i = 1; i <= 5; i++) {
                if (sink.isCancelled()) break; // 可选：提前检查取消
                sink.next("Item-" + i);
            }
            sink.complete(); // 必须调用，否则订阅者会一直等待
        });
        customFlux.subscribe(System.out::println);
    }
    //endregion

    //region 2. 订阅与消费：如何接收数据
    public static void subscribeFlux() {
        System.out.println("\n=== 2. 订阅 Flux ===");

        Flux<String> flux = Flux.just("Hello", "Reactor");

        // 2.1 最简订阅：只处理 onNext
        flux.subscribe(System.out::println);

        // 2.2 完整订阅：处理 onNext, onError, onComplete
        flux.subscribe(
                data -> System.out.println("Data: " + data),      // onNext
                error -> System.err.println("Error: " + error),   // onError
                () -> System.out.println("Completed!")             // onComplete
        );

        // 2.3 带 Context 的订阅（高级，用于传递上下文）
        flux.contextWrite(ctx -> ctx.put("user", "alice"))
                .subscribe(data -> {
                    // 可通过 context 获取，但通常在操作符内部使用
                    System.out.println("With context: " + data);
                });
    }
    //endregion

    //region 3. 转换与操作：map, filter, flatMap 等
    public static void transformFlux() {
        System.out.println("\n=== 3. 转换 Flux ===");

        Flux<Integer> numbers = Flux.range(1, 5); // 1,2,3,4,5

        // 3.1 map：一对一转换
        numbers.map(n -> n * 2)
                .subscribe(n -> System.out.print(n + " ")); // 2 4 6 8 10

        System.out.println();

        // 3.2 filter：过滤
        numbers.filter(n -> n % 2 == 0)
                .subscribe(n -> System.out.print(n + " ")); // 2 4

        System.out.println();

        // 3.3 flatMap：一对多 or 异步转换（关键！）
        numbers.flatMap(n ->
                Mono.just(n * 10).delayElement(Duration.ofMillis(100)) // 模拟异步
        ).subscribe(n -> System.out.print(n + " ")); // 10 20 30 40 50（异步顺序可能乱）

        System.out.println();
    }
    //endregion

    //region 4. 错误处理：onErrorReturn, retry, doOnError
    public static void errorHandling() {
        System.out.println("\n=== 4. 错误处理 ===");

        Flux<String> riskyFlux = Flux.just("A", "B")
                .concatWith(Flux.error(new RuntimeException("Fail!")))
                .concatWith(Flux.just("C")); // 这行不会执行

        // 4.1 捕获错误并返回默认值
        riskyFlux.onErrorReturn("Fallback")
                .subscribe(System.out::println); // A, B, Fallback

        // 4.2 重试（最多3次）
        Flux<String> retryFlux = Flux.defer(() -> {
            AtomicInteger count = new AtomicInteger();
            return Flux.just("Try")
                    .doOnNext(s -> {
                        if (count.incrementAndGet() < 3) {
                            throw new RuntimeException("Retryable error");
                        }
                    });
        });
        retryFlux.retry(2) // 重试2次（共尝试3次）
                .subscribe(
                        System.out::println,
                        err -> System.err.println("最终失败: " + err)
                );
    }
    //endregion

    //region 5. 取消机制（Cancellation）—— 关键！用于“停止生成”
    public static void cancellationDemo() {
        System.out.println("\n=== 5. 取消机制 onCancel ===");

        // 模拟一个长时间运行的任务（如 AI 生成）
        Flux<String> longRunningFlux = Flux.create(sink -> {
            AtomicBoolean cancelled = new AtomicBoolean(false);

            // 注册取消回调：当客户端断开或主动取消时触发
            sink.onCancel(() -> {
                System.out.println("⚠️ 流被取消！正在清理资源...");
                cancelled.set(true);
                // 这里可以：关闭数据库连接、中断线程、通知 LLM 停止等
            });

            // 模拟逐块生成数据
            for (int i = 1; i <= 10; i++) {
                if (cancelled.get()) {
                    System.out.println("✅ 已响应取消信号，提前退出");
                    return; // 真正中断逻辑
                }
                try {
                    Thread.sleep(500); // 模拟耗时
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                sink.next("Chunk-" + i);
            }
            sink.complete();
        });

        // 订阅并手动取消
        var disposable = longRunningFlux.subscribe(System.out::println);

        // 2秒后取消
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        disposable.dispose(); // 触发 onCancel

        // 等待一点时间看效果
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    //endregion

    //region 6. 背压（Backpressure）—— 控制生产速度
    public static void backpressureDemo() {
        System.out.println("\n=== 6. 背压控制 ===");

        // 快速生产者 vs 慢速消费者
        Flux<Integer> fastProducer = Flux.range(1, 1000)
                .doOnNext(i -> System.out.println("Produced: " + i));

        // 使用 onBackpressureBuffer 缓冲（默认256）
        fastProducer
                .onBackpressureBuffer(100, o -> System.out.println("Dropped: " + o)) // 超出则丢弃
                .delayElements(Duration.ofMillis(100)) // 模拟慢消费
                .subscribe(
                        i -> System.out.println("Consumed: " + i),
                        Throwable::printStackTrace,
                        () -> System.out.println("Done")
                );

        // 注意：WebFlux 中 Spring 会自动处理背压（基于 HTTP 流）
    }
    //endregion

    //region 7. 线程调度：publishOn / subscribeOn
    public static void schedulerDemo() {
        System.out.println("\n=== 7. 线程调度 ===");

        Flux.just("Task-1", "Task-2")
                .doOnNext(s -> System.out.println("Emit on: " + Thread.currentThread().getName()))
                .publishOn(Schedulers.boundedElastic()) // 切换到弹性线程池（用于阻塞操作）
                .map(s -> {
                    System.out.println("Process on: " + Thread.currentThread().getName());
                    return s.toUpperCase();
                })
                .subscribeOn(Schedulers.parallel()) // 订阅发生在 parallel 线程
                .subscribe(s -> System.out.println("Receive on: " + Thread.currentThread().getName()));
    }
    //endregion

    //region 主方法：运行所有示例
    public static void main(String[] args) throws InterruptedException {
        createFlux();
        subscribeFlux();
        transformFlux();
        errorHandling();
        cancellationDemo();
        backpressureDemo();
        schedulerDemo();

        // 等待异步任务完成（仅用于 demo）
        Thread.sleep(5000);
    }
    //endregion
}
