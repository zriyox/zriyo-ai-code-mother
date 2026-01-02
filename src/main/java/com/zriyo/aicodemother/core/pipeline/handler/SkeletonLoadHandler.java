package com.zriyo.aicodemother.core.pipeline.handler;


import com.zriyo.aicodemother.ai.service.AiCodeGenTypeRoutingServiceImpl;
import com.zriyo.aicodemother.core.pipeline.GenerationContext;
import com.zriyo.aicodemother.core.pipeline.service.CodeGenRecordService;
import com.zriyo.aicodemother.model.dto.ProjectSkeletonDTO;
import com.zriyo.aicodemother.model.enums.AiCodeGenStage;
import com.zriyo.aicodemother.model.enums.ChatHistoryMessageTypeEnum;
import com.zriyo.aicodemother.model.message.StreamMessageTypeEnum;
import com.zriyo.aicodemother.service.AiToolLogService;
import com.zriyo.aicodemother.service.ChatHistoryService;
import com.zriyo.aicodemother.util.SseEventBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.annotation.Order;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Objects;

@Component
@Order(1)
@Slf4j
public class SkeletonLoadHandler extends AbstractCodeGenHandler {

    private final ChatHistoryService chatHistoryService;

    public SkeletonLoadHandler(CodeGenRecordService codeGenRecordService, ChatHistoryService chatHistoryService, AiToolLogService aiToolLogService, ApplicationEventPublisher publisher
            , AiCodeGenTypeRoutingServiceImpl aiCodeGenTypeRoutingService) {
        super(codeGenRecordService, chatHistoryService, aiToolLogService, publisher, aiCodeGenTypeRoutingService);
        this.chatHistoryService = chatHistoryService;
    }

    @Override
    protected AiCodeGenStage getStage() {
        return AiCodeGenStage.LOAD_SKELETON;
    }

    @Override
    protected boolean shouldSkip(GenerationContext context) {
        return context.getIsFirstBuild();
    }

    @Override
    protected Flux<ServerSentEvent<Object>> doExecute(GenerationContext context) {

        return Flux.concat(
                Flux.just(SseEventBuilder.of(StreamMessageTypeEnum.AI_RESPONSE, "正在分析用户需求...")),
                Flux.just(SseEventBuilder.of(StreamMessageTypeEnum.TOOL_REQUEST, "🔧 正在加载现有项目结构...")),
                Mono.fromCallable(() -> {
                            ProjectSkeletonDTO skeleton = chatHistoryService.getLastSkeletonByType(
                                    context.getAppId(),
                                    context.getUserId(),
                                    ChatHistoryMessageTypeEnum.SKELETON.getValue(), context
                            );
                            if (Objects.nonNull(context.getRuntimeFeedback())){
                                Long toolId = ErrFixMessage(context);
                                context.setToolMassageId(toolId);
                            }
                            if (skeleton == null) {
                                throw new IllegalStateException("未找到可编辑的项目结构，请先创建项目");
                            }
                            return skeleton;
                        })
                        .subscribeOn(Schedulers.boundedElastic())
                        .doOnNext(skeleton -> {
                            // 设置上下文
                            context.setSkeleton(skeleton);

                        })
                        .flatMapMany(skeleton ->
                                Flux.just(
                                        SseEventBuilder.of(StreamMessageTypeEnum.TOOL_EXECUTED, "✅ **项目结构加载完成**，准备编辑...")
                                )
                        )
                        .onErrorResume(e -> {
                                    context.setIsError(true);
                                    context.setTerminated(true);
                                    return Flux.just(SseEventBuilder.of(StreamMessageTypeEnum.ERROR, "加载失败: " + e.getMessage()));
                                }
                        )
        );
    }
}
