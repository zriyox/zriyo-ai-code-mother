package com.zriyo.aicodemother.service;

import com.mybatisflex.core.service.IService;
import com.zriyo.aicodemother.model.entity.UserPoints;

/**
 * 用户积分账户管理服务
 * 职责：初始化、查询、确保存在
 */
public interface PointsAccountService extends IService<UserPoints> {

    /**
     * 获取用户积分账户，若不存在则自动创建（幂等）
     */
    UserPoints getOrCreateUserPoints(Long userId);

    /**
     * 仅从数据库查询，不创建
     */
    UserPoints getUserPoints(Long userId);

    /**
     * 显式初始化用户积分账户（通常由系统调用，对外可不暴露）
     */
    boolean initUserPoints(Long userId);
}
