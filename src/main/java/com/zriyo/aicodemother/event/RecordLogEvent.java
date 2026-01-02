package com.zriyo.aicodemother.event;

import com.zriyo.aicodemother.model.entity.AiCodeGenRecord;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;
@Getter
public class RecordLogEvent extends ApplicationEvent {
    private final AiCodeGenRecord aiCodeGenRecords;

    public RecordLogEvent(Object source, AiCodeGenRecord aiCodeGenRecords) {
        super(source);
        this.aiCodeGenRecords = aiCodeGenRecords;
    }
}
