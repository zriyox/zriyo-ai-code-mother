package com.zriyo.aicodemother.mapper;

import com.mybatisflex.core.BaseMapper;
import com.zriyo.aicodemother.model.entity.DeploymentHistory;

import java.util.List;

/**
 * 应用部署历史记录 映射层。
 *
 */
public interface DeploymentHistoryMapper extends BaseMapper<DeploymentHistory> {

    DeploymentHistory selectOneNewData(Long id);

    List<DeploymentHistory> getDeploymentHistoryWithCursorPagination(Long id);

    DeploymentHistory selectLatestByAppId(Long id);
}
