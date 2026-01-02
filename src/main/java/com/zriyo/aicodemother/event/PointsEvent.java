package com.zriyo.aicodemother.event;

import com.zriyo.aicodemother.model.enums.PointsReasonEnum;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class PointsEvent extends ApplicationEvent {
    private final Long id;
    private final Long userId;
    private final PointsReasonEnum reason;

    public PointsEvent(Object source, Long id, Long userId, PointsReasonEnum reason) {
        super(source);
        this.id = id;
        this.userId = userId;
        this.reason = reason;
    }
}
