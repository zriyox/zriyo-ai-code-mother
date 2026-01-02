package com.zriyo.aicodemother.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import com.zriyo.aicodemother.common.PageRequest;
import com.zriyo.aicodemother.model.entity.PointsLog;
import com.zriyo.aicodemother.model.vo.PointsLogPage;

/**
 * 积分变动流水日志（逻辑外键，全量可追溯） 服务层。
 *
 */
public interface PointsLogService extends IService<PointsLog> {

    /**
     * 构建积分变动流水日志
     */
    PointsLog buildPointsLog(Long userId, Integer changeAmount, String reason, Long relatedId);

    /**
     * 获取用户积分变动流水日志
     */
    Page<PointsLogPage> getUserPointsHistory(Long userId, PageRequest pageRequest);


}
