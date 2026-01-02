package com.zriyo.aicodemother.mapper;

import com.mybatisflex.core.BaseMapper;
import com.zriyo.aicodemother.model.entity.UserPoints;

/**
 * 用户积分账户（逻辑外键 + 乐观锁） 映射层。
 *
 */
public interface UserPointsMapper extends BaseMapper<UserPoints> {

    boolean deductPoints(Long userId, Integer fixedPoints,Integer currentVersion );

    public boolean adjustPoints(Long userId, int delta, int absDelta, Integer version);
}
