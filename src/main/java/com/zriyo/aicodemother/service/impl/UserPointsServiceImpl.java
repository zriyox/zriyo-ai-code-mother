// com.zriyo.aicodemother.service.impl.UserPointsServiceImpl
package com.zriyo.aicodemother.service.impl;

import com.zriyo.aicodemother.model.entity.UserPoints;
import com.zriyo.aicodemother.model.vo.UserPointsVo;
import com.zriyo.aicodemother.service.PointsAccountService;
import com.zriyo.aicodemother.service.SignService;
import com.zriyo.aicodemother.service.UserPointsService;
import com.zriyo.aicodemother.util.BeanCopyUtil;
import com.zriyo.aicodemother.util.RedisUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserPointsServiceImpl implements UserPointsService {

    private final PointsAccountService pointsAccountService;
    private final SignService signService;

    private static final String POINTS_RANKING_KEY = "points:ranking";

    @Override
    public UserPointsVo getUserPointsVo(Long userId) {
        UserPoints userPoints = pointsAccountService.getUserPoints(userId);
        if (userPoints == null) {
            pointsAccountService.initUserPoints(userId);
            userPoints = pointsAccountService.getUserPoints(userId);
        }
        UserPointsVo vo = BeanCopyUtil.copy(userPoints, UserPointsVo.class);
        vo.setIsSign(signService.isSignedToday(userId));
        return vo;
    }

    @Override
    public Long getUserRank(Long userId) {
        Integer rank0Based = RedisUtils.zRevRank(POINTS_RANKING_KEY, userId.toString());
        if (rank0Based == null) {
            return null;
        }
        return (long) rank0Based + 1;
    }
}
