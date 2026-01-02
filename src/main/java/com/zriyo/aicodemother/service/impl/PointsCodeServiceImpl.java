package com.zriyo.aicodemother.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.zriyo.aicodemother.mapper.PointsCodeMapper;
import com.zriyo.aicodemother.model.entity.PointsCode;
import com.zriyo.aicodemother.service.PointsCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 积分兑换码管理表（逻辑外键，支持批量发放与追踪） 服务层实现。
 *
 */
@Service
@RequiredArgsConstructor
public class PointsCodeServiceImpl extends ServiceImpl<PointsCodeMapper, PointsCode>  implements PointsCodeService {
    private final PointsCodeMapper pointsCodeMapper;

    @Override
    public PointsCode getByCdk(String cdk) {
        QueryWrapper queryWrapper = new QueryWrapper().eq(PointsCode::getCode, cdk);
        return this.getOne(queryWrapper);
    }
}
