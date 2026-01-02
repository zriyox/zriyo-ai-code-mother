package com.zriyo.aicodemother.service;

import com.mybatisflex.core.service.IService;
import com.zriyo.aicodemother.model.entity.PointsCode;

/**
 * 积分兑换码管理表（逻辑外键，支持批量发放与追踪） 服务层。
 *
 */
public interface PointsCodeService extends IService<PointsCode> {

    PointsCode getByCdk(String cdk);
}
