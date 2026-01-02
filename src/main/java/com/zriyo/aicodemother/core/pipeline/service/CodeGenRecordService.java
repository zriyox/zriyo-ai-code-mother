package com.zriyo.aicodemother.core.pipeline.service;

import com.zriyo.aicodemother.core.pipeline.GenerationContext;
import com.zriyo.aicodemother.event.RecordLogEvent;
import com.zriyo.aicodemother.model.entity.AiCodeGenRecord;
import com.zriyo.aicodemother.model.enums.AiCodeGenStage;
import com.zriyo.aicodemother.model.enums.AiCodeGenStatus;
import lombok.AllArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@AllArgsConstructor
public class CodeGenRecordService {

    private final ApplicationEventPublisher publisher;

    public AiCodeGenRecord start(GenerationContext context, AiCodeGenStage stage) {
        AiCodeGenRecord build = build(context, stage, AiCodeGenStatus.RUNNING);
        context.setRecord(build);
        return build;
    }

    public void success(GenerationContext context) {
        AiCodeGenRecord r = finish(context, AiCodeGenStatus.SUCCESS);
        publisher.publishEvent(new RecordLogEvent(this, r));
    }

    public void fail(GenerationContext context, String reason) {
        AiCodeGenRecord r = finish(context, AiCodeGenStatus.FAILED);
        r.setErrorMessage(reason);
        publisher.publishEvent(new RecordLogEvent(this, r));
    }

    private AiCodeGenRecord finish(GenerationContext context, AiCodeGenStatus status) {
        AiCodeGenRecord r = context.getRecord();
        r.setStatus(status.getValue());
        r.setEndTime(LocalDateTime.now());
        return r;
    }

    private AiCodeGenRecord build(GenerationContext ctx, AiCodeGenStage stage, AiCodeGenStatus status) {
        AiCodeGenRecord r = new AiCodeGenRecord();
        r.setAppId(ctx.getAppId());
        r.setUserId(ctx.getUserId());
        r.setStage(stage.getValue());
        r.setStatus(status.getValue());
        r.setStartTime(LocalDateTime.now());
        r.setProjectDir(ctx.getProjectDir());
        r.setMessageId(ctx.getMessageId());
        return r;
    }
}
