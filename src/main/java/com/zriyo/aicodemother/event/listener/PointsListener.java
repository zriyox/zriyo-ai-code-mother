package com.zriyo.aicodemother.event.listener;

import com.zriyo.aicodemother.config.AsyncConfig;
import com.zriyo.aicodemother.event.PointsEvent;
import com.zriyo.aicodemother.model.enums.PointsReasonEnum;
import com.zriyo.aicodemother.service.PointsAdjustService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor

public class PointsListener {

    private final PointsAdjustService pointsAdjustService;

    @EventListener(classes = PointsEvent.class)
    @Async(AsyncConfig.AI_ASYNC_EXECUTOR)
    public void handle(PointsEvent event) {
        Long id = event.getId();
        Long userId = event.getUserId();
        PointsReasonEnum reason = event.getReason();
        pointsAdjustService.adjustPoints(userId, reason, id,null);
    }
}
