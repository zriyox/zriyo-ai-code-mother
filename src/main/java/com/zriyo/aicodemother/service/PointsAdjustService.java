// com.zriyo.aicodemother.service.points.PointsAdjustService
package com.zriyo.aicodemother.service;

import com.zriyo.aicodemother.model.enums.PointsReasonEnum;

public interface PointsAdjustService {

    boolean adjustPoints(Long userId, PointsReasonEnum reason, Long relatedId, Integer dealt);
    boolean adjustPoints(Long userId, PointsReasonEnum reason, Long relatedId, Integer dealt,String key);

    void validatePoints(Long loginId);
}
