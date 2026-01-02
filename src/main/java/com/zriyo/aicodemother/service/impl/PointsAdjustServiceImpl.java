// com.zriyo.aicodemother.service.points.impl.PointsAdjustServiceImpl
package com.zriyo.aicodemother.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.zriyo.aicodemother.exception.BusinessException;
import com.zriyo.aicodemother.exception.ErrorCode;
import com.zriyo.aicodemother.mapper.UserPointsMapper;
import com.zriyo.aicodemother.model.entity.PointsLog;
import com.zriyo.aicodemother.model.entity.UserPoints;
import com.zriyo.aicodemother.model.enums.PointsReasonEnum;
import com.zriyo.aicodemother.service.PointsAccountService;
import com.zriyo.aicodemother.service.PointsAdjustService;
import com.zriyo.aicodemother.service.PointsLogService;
import com.zriyo.aicodemother.util.RedisUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PointsAdjustServiceImpl implements PointsAdjustService {

    private final UserPointsMapper userPointsMapper;
    private final PointsLogService pointsLogService;
    private final PointsAccountService pointsAccountService;

    private static final String POINTS_RANKING_KEY = "points:ranking";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean adjustPoints(Long userId, PointsReasonEnum reason, Long relatedId, Integer dealt) {
        return instPoints(userId, reason, relatedId, dealt, null);
    }

    private boolean instPoints(Long userId, PointsReasonEnum reason, Long relatedId, Integer dealt, String key) {
        UserPoints userPoints = pointsAccountService.getOrCreateUserPoints(userId);

        int delta = reason.getFixedPoints();
        if (dealt != null) {
            delta = dealt;
        }
        int absDelta = Math.abs(delta);
        // 构建日志（先不保存）
        PointsLog pointsLog = pointsLogService.buildPointsLog(userId, delta, reason.getCode(), relatedId);

        // 执行数据库更新（带 version 乐观锁）
        boolean updated = userPointsMapper.adjustPoints(
                userId,
                delta,
                absDelta,
                userPoints.getVersion()
        );

        if (!updated) {
            UserPoints latest = pointsAccountService.getUserPoints(userId);
            if (delta < 0 && latest != null && latest.getAvailablePoints() < absDelta) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_POINTS);
            } else {
                throw new BusinessException(ErrorCode.TOO_FAST);
            }
        }

        UserPoints refreshed = pointsAccountService.getUserPoints(userId);
        if (refreshed == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新后用户积分记录丢失");
        }
        pointsLog.setBalanceAfter(refreshed.getAvailablePoints());

        // 保存日志
        if (!pointsLogService.save(pointsLog)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }

        // 更新 Redis 排行榜
        RedisUtils.zAdd(POINTS_RANKING_KEY, refreshed.getAvailablePoints(), userId.toString());

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean adjustPoints(Long userId, PointsReasonEnum reason, Long relatedId, Integer dealt, String key) {
        return instPoints(userId, reason, relatedId, dealt, key);
    }

    @Override
    public void validatePoints(Long loginId) {
        UserPoints userPoints = userPointsMapper.selectOneByQuery(new QueryWrapper().eq(UserPoints::getUserId, loginId));
        if (userPoints == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (userPoints.getAvailablePoints() <= 0){
            throw new BusinessException(ErrorCode.INSUFFICIENT_POINTS);
        }
    }
}
