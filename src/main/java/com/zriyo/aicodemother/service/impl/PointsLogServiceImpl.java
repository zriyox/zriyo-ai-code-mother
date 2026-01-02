package com.zriyo.aicodemother.service.impl;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.zriyo.aicodemother.common.PageRequest;
import com.zriyo.aicodemother.mapper.PointsLogMapper;
import com.zriyo.aicodemother.model.entity.PointsLog;
import com.zriyo.aicodemother.model.enums.PointsReasonEnum;
import com.zriyo.aicodemother.model.vo.PointsLogPage;
import com.zriyo.aicodemother.service.PointsLogService;
import com.zriyo.aicodemother.util.BeanCopyUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 积分变动流水日志（逻辑外键，全量可追溯） 服务层实现。
 *
 */
@Service
@RequiredArgsConstructor
public class PointsLogServiceImpl extends ServiceImpl<PointsLogMapper, PointsLog>  implements PointsLogService {
    private final PointsLogMapper pointsLogMapper;
    @Override
    public PointsLog buildPointsLog(Long userId, Integer changeAmount, String reason, Long relatedId) {
        return PointsLog.builder()
                .userId(userId)
                .changeAmount(changeAmount)
                .reason(reason)
                .balanceAfter(changeAmount)
                .relatedId(relatedId)
                .build();
    }

    @Override
    public Page<PointsLogPage> getUserPointsHistory(Long userId, PageRequest pageRequest) {
        QueryWrapper queryWrapper = this.getQueryWrapper(userId);
        Page<PointsLog> pointsLogPage = this.page(Page.of(pageRequest.getPageNum(), pageRequest.getPageSize()), queryWrapper);
        Page<PointsLogPage> pointsLogPagePage = new Page<>(pageRequest.getPageNum(), pageRequest.getPageSize(), pointsLogPage.getTotalRow());
        List<PointsLogPage> pointsLogPages = BeanCopyUtil.convertToList(pointsLogPage.getRecords(), PointsLogPage.class, list->{
            list.setReason(PointsReasonEnum.fromCode(list.getReason()).getDescription());
        });
        pointsLogPagePage.setRecords(pointsLogPages);
        return pointsLogPagePage;
    }
    public QueryWrapper getQueryWrapper(Long userId) {
        return QueryWrapper.create()
                .eq("user_id", userId)
                .orderBy(PointsLog::getCreatedAt, false);
    }


}
