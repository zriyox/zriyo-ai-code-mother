package com.zriyo.aicodemother.core.pipeline.handler;

import cn.hutool.json.JSONUtil;
import com.zriyo.aicodemother.ai.service.AiCodeGenTypeRoutingServiceImpl;
import com.zriyo.aicodemother.core.handler.AiContextHolder;
import com.zriyo.aicodemother.core.pipeline.GenerationContext;
import com.zriyo.aicodemother.core.pipeline.service.CodeGenRecordService;
import com.zriyo.aicodemother.model.MonitorContext;
import com.zriyo.aicodemother.model.dto.ProjectSkeletonDTO;
import com.zriyo.aicodemother.model.dto.chat.ChatMessage;
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

@Component
@Order(1)
@Slf4j
public class SkeletonGenerateHandler extends AbstractCodeGenHandler {

    public SkeletonGenerateHandler(AiCodeGenTypeRoutingServiceImpl aiCodeGenTypeRoutingService,
                                   CodeGenRecordService codeGenRecordService,
                                   ChatHistoryService chatHistoryService,
                                   AiToolLogService aiToolLogService,
                                   ApplicationEventPublisher publisher) {
        super(codeGenRecordService, chatHistoryService, aiToolLogService, publisher, aiCodeGenTypeRoutingService);
    }

    @Override
    protected AiCodeGenStage getStage() {
        return AiCodeGenStage.SKELETON;
    }

    @Override
    protected boolean shouldSkip(GenerationContext context) {
        return !context.getIsFirstBuild() || context.getRuntimeFeedback() != null;
    }

    @Override
    protected Flux<ServerSentEvent<Object>> doExecute(GenerationContext context) {
        if (stopGeneration(context)) {
            return stopMessage();
        }

        return Flux.concat(
                Flux.just(SseEventBuilder.of(StreamMessageTypeEnum.TOOL_REQUEST, "🧠 分析需求并规划文件目录...")),
                Mono.fromCallable(() -> {

                            AiContextHolder.set(MonitorContext.builder()
                                    .appId(String.valueOf(context.getAppId()))
                                    .userId(String.valueOf(context.getUserId()))
                                    .build());

                            return invokeCodeGenType(context, SKELETON, context.getMessage());
                        })
                        .subscribeOn(Schedulers.boundedElastic()) // 切换线程池
                        .doOnNext(skeleton -> {

                            AiContextHolder.set(MonitorContext.builder()
                                    .appId(String.valueOf(context.getAppId()))
                                    .userId(String.valueOf(context.getUserId()))
                                    .build());

                            context.setSkeleton((ProjectSkeletonDTO) skeleton);
                            ChatMessage chatMessage = new ChatMessage();
                            chatMessage.setMessage(JSONUtil.toJsonStr(skeleton));
                            chatMessage.setMessageType(ChatHistoryMessageTypeEnum.SKELETON.getValue());
                            chatMessage.setAppId(context.getAppId());
                            chatMessage.setUserVisible(0);
                            chatHistoryService.addChatMessage(chatMessage, context.getUserId());
                        })
                        .flatMapMany(skeleton ->
                                Flux.just(SseEventBuilder.of(StreamMessageTypeEnum.TOOL_EXECUTED, "📁 需求分析完毕!"))
                        )
                        .onErrorResume(e -> {
                            context.setIsError(true);
                            context.setTerminated(true);
                            return Flux.just(
                                    SseEventBuilder.of(StreamMessageTypeEnum.ERROR, "骨架生成失败: " + e.getMessage())
                            );
                        })
        );
    }
}
