package com.zriyo.aicodemother.service;

import com.mybatisflex.core.service.IService;
import com.zriyo.aicodemother.model.entity.App;
import com.zriyo.aicodemother.model.entity.DeploymentHistory;

import java.util.List;

/**
 * 应用部署历史记录 服务层。
 *
 */
public interface DeploymentHistoryService extends IService<DeploymentHistory> {

    void recordDeployment(App app, String deployName, String initialVersion);

    List<DeploymentHistory> getDeploymentHistoryWithCursorPagination(Long id);


}
