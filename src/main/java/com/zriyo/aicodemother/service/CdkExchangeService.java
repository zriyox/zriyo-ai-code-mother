// com.zriyo.aicodemother.service.cdk.CdkExchangeService
package com.zriyo.aicodemother.service;

import com.zriyo.aicodemother.exception.BusinessException;
import com.zriyo.aicodemother.exception.ErrorCode;
import com.zriyo.aicodemother.model.entity.PointsCode;
import com.zriyo.aicodemother.model.enums.PointsReasonEnum;
import com.zriyo.aicodemother.util.RedisUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CdkExchangeService {

    private final PointsCodeService pointsCodeService;
    private final PointsAdjustService pointsAdjustService;

    @Transactional(rollbackFor = Exception.class)
    public Boolean exchangeCdk(Long userId, String cdk) {
        RedisUtils.executeWithLock(cdk, 5000, () -> {
            PointsCode pointsCode = pointsCodeService.getByCdk(cdk);
            if (pointsCode == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
            }
            if (pointsCode.getStatus() != 0) {
                throw new BusinessException(ErrorCode.CDK_ALREADY_EXCHANGED);
            }

            boolean adjusted = pointsAdjustService.adjustPoints(
                    userId,
                    PointsReasonEnum.REDEEM_CODE,
                    pointsCode.getId(),
                    pointsCode.getPoints(),cdk
            );
            if (!adjusted) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR);
            }

            pointsCode.setStatus(1);
            if (!pointsCodeService.updateById(pointsCode)) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR);
            }
        });
        return true;
    }
}
