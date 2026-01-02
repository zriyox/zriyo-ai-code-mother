# Project Reactor 实战开发指南

> **文档说明**：本文档基于 ZriyoCode 项目实际代码开发编写，旨在帮助开发人员从零快速掌握响应式编程。
> 内容涵盖：基础入门、项目核心模式、以及高级进阶 API。

## 一、基础入门 (Zero to One)

如果你是从 `List` 和 `for` 循环转过来的，请先建立以下映射概念。

### 1.1 创建流 (Creation)

流是数据的源头。

*   **`Mono.just(T data)`**: 创建一个包含单个数据的流。
    ```java
    Mono.just("Hello"); // 类似于 Optional.of("Hello")
    ```
*   **`Flux.just(T... data)`**: 创建包含多个固定数据的流。
    ```java
    Flux.just("A", "B", "C"); // 类似于 Stream.of("A", "B", "C")
    ```
*   **`Flux.fromIterable(List<T> list)`**: 将 Java 集合转为流。 [最常用]
    ```java
    List<String> names = Arrays.asList("Bob", "Alice");
    Flux.fromIterable(names);
    ```
*   **`Mono.empty()` / `Flux.empty()`**: 创建一个空流（没有数据，直接结束）。常用于返回 `void` 的场景。
*   **`Mono.error(Throwable e)`**: 直接抛出异常的流。

### 1.2 中间处理 (Transformation)

像流水线工人一样加工经过的数据。

*   **`map` (1对1转换)**: 拿到数据，改一下，传给下游。
    ```java
    Flux.just(1, 2)
        .map(i -> "Number: " + i); // 1 -> "Number: 1"
    ```
*   **`flatMap` (异步展开)**: 拿到数据，把它变成一个新的流（比如去查数据库），然后合并。
    ```java
    Flux.just(userId)
        .flatMap(id -> userService.findById(id)); // 适合异步 I/O 操作
    ```
*   **`filter` (过滤)**: 只要满足条件的数据。
    ```java
    Flux.just(1, 2, 3)
        .filter(i -> i > 1); // 结果: 2, 3
    ```

### 1.3 观察与副作用 (Peeking)

在不改变数据流的前提下，"偷看"一眼或做点 side-work。

*   **`doOnNext`**: 每次有新数据通过时（做日志）。
*   **`doOnError`**: 发生错误时。
*   **`doOnSubscribe`**: 流被订阅的那一刻（任务开始时）。
*   **`doFinally`**: 流结束时（无论成功失败，做资源清理）。

### 1.4 订阅与阻塞 (Consumption)

流建立好后，如果不“订阅”，它里面一行代码都不会跑（懒加载特性）。

*   **`.subscribe()`**: 触发流执行，但**不阻塞**当前线程。 (Fire-and-Forget)
*   **`.block()`**: **阻塞**当前线程，直到流结束并在拿到结果。 **(禁止在 Controller/Service 中使用，会卡死服务器！仅限单元测试)**

---

## 二、核心概念速查 (Core Concepts)

| 核心类 | 简述 | 类比 (Java Stream) |通过代码理解 |
| :--- | :--- | :--- | :--- |
| **`Mono<T>`** | 包含 **0 或 1** 个元素的异步序列 | `Optional<CompletableFuture<T>>` | `Mono.just("Hello")` |
| **`Flux<T>`** | 包含 **0 到 N** 个元素的异步序列 | `Stream<CompletableFuture<T>>` | `Flux.fromIterable(list)` |
| **`Sink`** | 手动触发数据推送的"发射器" | `BlockingQueue` + `Publisher` | `Sinks.many().tryEmitNext()` |

---

## 三、项目中的高频模式 (Design Patterns)

### 3.1 异步任务流水线 (Async Pipeline)

**场景**：`AppServiceImpl.java` 中，我们希望在后台启动一个耗时的代码生成任务，而不阻塞当前的 HTTP 请求线程。

**代码模式**：
```java
// 1. 使用 Flux.defer 包裹任务创建逻辑，实现懒加载（只有订阅时才创建）
Flux.defer(() -> {
    // 2. 这里是业务逻辑，比如构建责任链
    return pipeline.handle(context);
})
// 3. 关键：Schedulers.boundedElastic() 指定在弹性线程池运行
// 专门用于 IO 密集型任务（如读写文件、调 OpenAI、查数据库）
.subscribeOn(Schedulers.boundedElastic()) 
.subscribe(); // 4. 立即触发执行（Fire-and-Forget）
```
> **原理**：`subscribeOn` 改变了上游（任务生成）的执行线程，实现了真正的异步化。

### 3.2 全量重放缓存 (Replayable Cache)

**场景**：SSE 断线重连。前 10 秒产生的日志，用户第 11 秒才连上来，如何让他看到之前的日志？

**代码模式**：
```java
// Sinks.many() -> 多播（支持多个人同时看）
// .replay() -> 重放（缓存历史数据）
// .all() -> 无限缓存（缓存所有，直到内存爆）
Sinks.Many<ServerSentEvent<Object>> sink = Sinks.many().replay().all();

// 推送数据 (Producer)
sink.tryEmitNext(event);

// 消费数据 (Consumer)
// asFlux() 拿到流，新用户订阅时，会自动收到之前 tryEmitNext 过的数据
Flux<ServerSentEvent> flux = sink.asFlux();
```

### 3.3 流的合并与心跳 (Merge & Heartbeat)

**场景**：`VueProjectSseHandler.java` 中，既要返回 AI 的生成内容，又要维持 HTTP 心跳防止断连。

**代码模式**：
```java
// 业务流
Flux<ServerSentEvent> mainFlux = ...;

// 心跳流：每 6 秒发一个 ping
Flux<ServerSentEvent> heartbeat = Flux.interval(Duration.ofSeconds(6))
    .map(tick -> ServerSentEvent.builder().data("ping").build())
    // takeUntilOther: 只要主业务流结束，心跳流也立马结束，防止泄露
    .takeUntilOther(mainFlux);

// 合并两个流：前端会交替收到 业务数据 和 ping
return Flux.merge(mainFlux, heartbeat);
```

### 3.4 顺序处理与类型转换 (ConcatMap)

**场景**：处理 OpenAI 返回的字符流。因为 AI 返回的 JSON 块必须按顺序拼装，不能乱序并行。

**代码模式 (区别于 flatMap)**：
```java
source.concatMap(chunk -> {
    // 处理每一个 chunk
    // concatMap 会等待前一个元素处理完（return Mono），再处理下一个
    // 保证了处理的严序性（Strict Ordering）
    return Mono.just(processedChunk);
})
```
*   **对比**：如果用 `flatMap`，多个 chunk 可能会并发处理，导致代码乱序。

### 3.5 错误处理 (Error Handling)

**场景**：流处理过程中某一步报错了（比如 JSON 解析失败），不能让整个 SSE 断开。

**代码模式**：
```java
flux.onErrorContinue((throwable, obj) -> {
    // 记录日志，但是忽略这个错误，继续处理下一个元素
    log.warn("解析失败: {}, 跳过", throwable.getMessage());
})
```

### 3.6 流结束后的收尾 (Side Effects)

**场景**：流生成完毕后，需要把生成的总代码保存到数据库，或者触发 ApplicationEvent。

**代码模式**：
```java
flux.concatWith(Mono.defer(() -> {
    // concatWith: 等前面的流全部走完（onComplete）之后，执行这里的逻辑
    saveToDb(buffer.toString());
    return Mono.empty();
}));
```

---

## 四、常用操作符速查表 (Cheat Sheet)

| 操作符 | 作用 | 项目中的应用 |
| :--- | :--- | :--- |
| **`defer`** | 延迟创建流 | 只有当有订阅者时，才去执行上下文设置和 Pipeline 构建 |
| **`subscribeOn`** | 改变执行线程 | 将同步的生成逻辑扔到 `boundedElastic` 线程池 |
| **`share`** | 广播 (Multicast) | 让一个 OpenAI 流同时被 SSE 推送和日志记录器消费 |
| **`concatMap`** | 串行映射 | 解析 SSE 数据块，保证 JSON 组装顺序 |
| **`merge`** | 并发合并 | 合并业务流 + 心跳流 |
| **`doOnNext`** | 副作用 (Peek) | 每次产生数据时，顺便 log 一下或放入 Sink |
| **`doFinally`** | 最终清理 | 无论成功失败，最后都要清理 Redis 锁和 Map 缓存 |

---

## 五、高级 API 拓展 (Future Patterns)

虽然目前项目中未用到，但在复杂的微服务或高并发场景下，以下 API 极具威力，建议掌握。

### 5.1 聚合与压缩 (`zip` / `zipWith`)

**场景**：比如未来我们不仅要生成代码，还要同时生成对应的 API 文档和测试用例。这三个任务可以并行执行，但需要等**三个都完成后**，把结果打包返回给前端。

**代码示例**：
```java
Mono<String> codeMono = generateCode();
Mono<String> docMono = generateDoc();
Mono<String> testMono = generateTest();

// 等待所有任务完成，并将结果组合成一个 Tuple
Mono.zip(codeMono, docMono, testMono)
    .map(tuple -> {
        String code = tuple.getT1();
        String doc = tuple.getT2();
        String test = tuple.getT3();
        return new ProjectResult(code, doc, test);
    });
```

### 5.2 弹性重试 (`retry` / `retryWhen`)

**场景**：调用 OpenAI API 偶尔会超时或报 502。我们希望自动重试 3 次，且每次间隔指数递增（1s, 2s, 4s...）。

**代码示例**：
```java
openAiClient.chat("hello")
    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)) // 重试 3 次，初始间隔 1s
        .filter(throwable -> throwable instanceof TimeoutException) // 只在超时时重试
        .onRetry(retrySignal -> log.warn("Retrying... " + retrySignal.totalRetries()))
    );
```

### 5.3 缓冲与批处理 (`buffer` / `window`)

**场景**：假设 LLM 返回的字符流太快太碎（每次只返回 1 个字符），频繁写 DB 或推送前端会导致 CPU 飙升。我们可以每凑够 10 个字符或每过 100ms 处理一次。

**代码示例**：
```java
flux
    .bufferTimeout(10, Duration.ofMillis(100)) // 攒够 10 个或等待 100ms
    .flatMap(list -> {
        // list 是 List<String>，包含了多个 chunk
        String joined = String.join("", list);
        return writeToDb(joined); // 批量写入，降低 IO 压力
    });
```

### 5.4 超时控制 (`timeout`)

**场景**：如果生成任务超过 60 秒还在跑，强制中断并报错，防止僵尸任务拖垮系统。

**代码示例**：
```java
codeGenPipeline.handle(context)
    .timeout(Duration.ofSeconds(60), Mono.error(new TimeoutException("生成超时")))
    .onErrorResume(TimeoutException.class, e -> Mono.just("任务超时，已取消"));
```

### 5.5 并发控制 (`flatMap` + `concurrency`)

**场景**：如果你要批量生成 100 个文件，如果直接 `flatMap`，会瞬间发起 100 个 IO 请求把系统打挂。需要限制并发数为 5。

**代码示例**：
```java
Flux.fromIterable(files)
    .flatMap(file -> generateContent(file), 5) // 第二个参数控制最大并发数 (concurrency)
    .subscribe();
```
