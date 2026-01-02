package com.zriyo.aicodemother.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.zriyo.aicodemother.mapper.UserPointsMapper;
import com.zriyo.aicodemother.model.entity.UserPoints;
import com.zriyo.aicodemother.service.PointsAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 积分账户服务实现
 * 继承 MyBatis-Flex 的 ServiceImpl，复用 getById / save 等基础能力
 */
@Service
@RequiredArgsConstructor
public class PointsAccountServiceImpl extends ServiceImpl<UserPointsMapper, UserPoints>
        implements PointsAccountService {

    @Override
    public UserPoints getOrCreateUserPoints(Long userId) {
        UserPoints userPoints = this.getById(userId);
        if (userPoints == null) {
            initUserPoints(userId);
            return this.getById(userId); // 再次查询确保返回实体
        }
        return userPoints;
    }

    @Override
    public UserPoints getUserPoints(Long userId) {
        return this.getById(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean initUserPoints(Long userId) {
        // 幂等性检查：避免重复初始化
        if (this.getById(userId) != null) {
            return true;
        }
        UserPoints userPoints = UserPoints.builder()
                .userId(userId)
                .totalPoints(0)
                .availablePoints(0)
                .usedPoints(0)
                .version(0)
                .build();
        return this.save(userPoints);
    }
}
