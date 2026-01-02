package com.zriyo.aicodemother.event.listener;

import com.mybatisflex.core.query.QueryWrapper;
import com.zriyo.aicodemother.event.RecordLogEvent;
import com.zriyo.aicodemother.mapper.AiCodeGenRecordMapper;
import com.zriyo.aicodemother.model.entity.AiCodeGenRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@Slf4j
@RequiredArgsConstructor
public class RecordLogListener {
    private final AiCodeGenRecordMapper aiCodeGenRecordMapper;

    @EventListener(classes = RecordLogEvent.class)
    public void handle(RecordLogEvent event) {
        AiCodeGenRecord aiCodeGenRecords = event.getAiCodeGenRecords();
        AiCodeGenRecord aiCodeGenRecord = aiCodeGenRecordMapper.selectOneById(event.getAiCodeGenRecords().getAppId());
        if (Objects.isNull(aiCodeGenRecord)){
            aiCodeGenRecordMapper.insert(aiCodeGenRecords);
        }else {
            aiCodeGenRecordMapper.updateByQuery(aiCodeGenRecords
                    ,new QueryWrapper()
                    .eq(AiCodeGenRecord::getAppId,aiCodeGenRecords.getAppId()));
        }
    }
}
