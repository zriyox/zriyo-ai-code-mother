package com.zriyo.aicodemother.core.pipeline.handler;

import com.zriyo.aicodemother.ai.service.AiCodeGenTypeRoutingServiceImpl;
import com.zriyo.aicodemother.core.pipeline.GenerationContext;
import com.zriyo.aicodemother.core.pipeline.service.CodeGenRecordService;
import com.zriyo.aicodemother.model.AppConstant;
import com.zriyo.aicodemother.model.dto.ProjectSkeletonDTO;
import com.zriyo.aicodemother.model.enums.AiCodeGenStage;
import com.zriyo.aicodemother.model.message.StreamMessageTypeEnum;
import com.zriyo.aicodemother.service.AiToolLogService;
import com.zriyo.aicodemother.service.ChatHistoryService;
import com.zriyo.aicodemother.util.SseEventBuilder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.annotation.Order;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Component
@Order(2)
public class FileCreateHandler extends AbstractCodeGenHandler {


    public FileCreateHandler(CodeGenRecordService codeGenRecordService, ChatHistoryService chatHistoryService, AiToolLogService aiToolLogService, ApplicationEventPublisher publisher, AiCodeGenTypeRoutingServiceImpl aiCodeGenTypeRoutingService) {
        super(codeGenRecordService, chatHistoryService, aiToolLogService,publisher,aiCodeGenTypeRoutingService);
    }

    @Override
    protected AiCodeGenStage getStage() {
        return AiCodeGenStage.FILE_GENERATION;
    }

    @Override
    protected boolean shouldSkip(GenerationContext context) {
        return !context.getIsFirstBuild() || context.getRuntimeFeedback() != null;
    }

    @Override
    protected Flux<ServerSentEvent<Object>> doExecute(GenerationContext context) {
        Long appId = context.getAppId();
        ProjectSkeletonDTO skeleton = context.getSkeleton();
        if (skeleton == null || skeleton.getFiles() == null) {
            return Flux.error(new IllegalStateException("骨架数据为空，无法创建文件"));
        }
        Path baseDir = com.zriyo.aicodemother.util.CodeOutputManager.getCodeOutputBaseDir();
        String projectDirName = AppConstant.VUE_PROJECT_PREFIX + appId;
        Path projectRoot = baseDir.resolve(projectDirName);

        // 1. 发送“开始创建”事件
        Flux<ServerSentEvent<Object>> startEvent = Flux.just(
                SseEventBuilder.of(StreamMessageTypeEnum.TOOL_REQUEST, "🏗️ 正在创建项目基本骨架")
        );
        Long toolMessageId = createMessage(context);
        context.setToolMassageId(toolMessageId);
        boolean shopFlag = stopGeneration(context);
        if (shopFlag) {
            return stopMessage();
        }

        // 2. 创建所有空文件（异步）
        Flux<ServerSentEvent<Object>> fileCreationEvents = Flux.fromIterable(skeleton.getFiles().entrySet())
                .flatMap(entry -> {
                    String relativePath = entry.getKey();
                    return Mono.fromCallable(() -> {
                                createDirectoryIfNeeded(relativePath, projectRoot);
                                return SseEventBuilder.of(StreamMessageTypeEnum.TOOL_PROCESS, "创建文件: " + relativePath + " 成功！");
                            })
                            .subscribeOn(Schedulers.boundedElastic())
                            .onErrorResume(IOException.class, e -> {
                                context.setIsError(true);
                                context.setTerminated(true);
                                return Mono.just(SseEventBuilder.of(StreamMessageTypeEnum.ERROR,
                                        "创建文件失败: " + relativePath + ", 原因: " + e.getMessage()));
                            });
                });

        Flux<ServerSentEvent<Object>> templateCopyEvents = Mono.fromRunnable(() -> {
                    try {
                        copyOrReplacePackageJson(projectRoot);
                        copyOrReplaceTailwindJson(projectRoot);
                        copyOrReplacePostJson(projectRoot);
                        copyOrReplaceVitJson(projectRoot);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .thenMany(
                        Flux.just(SseEventBuilder.of(StreamMessageTypeEnum.TOOL_PROCESS, "移动配置文件模板文件已完成!")
                        ))
                .onErrorResume(Exception.class, e -> {
                    return Flux.just(SseEventBuilder.of(StreamMessageTypeEnum.ERROR,
                            "模板文件复制失败: " + e.getCause().getMessage()));
                });

        Flux<ServerSentEvent<Object>> doneEvent = Flux.just(
                SseEventBuilder.of(StreamMessageTypeEnum.TOOL_EXECUTED, "文件创建完毕!")
        );
        return Flux.concat(
                startEvent,
                fileCreationEvents,
                templateCopyEvents,
                doneEvent);
    }


    private void createDirectoryIfNeeded(String relativePath, Path projectRoot) throws IOException {
        Path fullPath = projectRoot.resolve(relativePath);
        Files.createDirectories(fullPath.getParent());
        if (!Files.exists(fullPath)) {
            Files.createFile(fullPath);
        }
    }

    private void copyOrReplacePackageJson(Path projectRoot) throws IOException {
        Path target = projectRoot.resolve("package.json");
        Files.createDirectories(target.getParent());
        String TEMPLATE_PACKAGE_JSON = com.zriyo.aicodemother.util.CodeOutputManager.getCodeOutputBaseDir()
                .resolve("config/package.json").toString();
        Files.copy(Paths.get(TEMPLATE_PACKAGE_JSON), target, StandardCopyOption.REPLACE_EXISTING);
    }

    private void copyOrReplaceTailwindJson(Path projectRoot) throws IOException {
        Path target = projectRoot.resolve("tailwind.config.js");
        Files.createDirectories(target.getParent());
        String TEMPLATE_TAILWIND_JSON = com.zriyo.aicodemother.util.CodeOutputManager.getCodeOutputBaseDir()
                .resolve("config/tailwind.config.js").toString();
        Files.copy(Paths.get(TEMPLATE_TAILWIND_JSON), target, StandardCopyOption.REPLACE_EXISTING);
    }

    private void copyOrReplacePostJson(Path projectRoot) throws IOException {
        Path target = projectRoot.resolve("postcss.config.js");
        Files.createDirectories(target.getParent());
        String TEMPLATE_POST_JSON = com.zriyo.aicodemother.util.CodeOutputManager.getCodeOutputBaseDir()
                .resolve("config/postcss.config.js").toString();
        Files.copy(Paths.get(TEMPLATE_POST_JSON), target, StandardCopyOption.REPLACE_EXISTING);
    }

    private void copyOrReplaceVitJson(Path projectRoot) throws IOException {
        Path target = projectRoot.resolve("vite.config.mjs");
        Files.createDirectories(target.getParent());
        String TEMPLATE_POST_JSON = com.zriyo.aicodemother.util.CodeOutputManager.getCodeOutputBaseDir()
                .resolve("config/vite.config.mjs").toString();
        Files.copy(Paths.get(TEMPLATE_POST_JSON), target, StandardCopyOption.REPLACE_EXISTING);
    }


}
