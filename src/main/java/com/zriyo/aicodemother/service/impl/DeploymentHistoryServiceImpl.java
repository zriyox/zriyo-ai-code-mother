package com.zriyo.aicodemother.service.impl;

import com.mybatisflex.core.util.UpdateEntity;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.zriyo.aicodemother.mapper.AppMapper;
import com.zriyo.aicodemother.mapper.DeploymentHistoryMapper;
import com.zriyo.aicodemother.model.entity.App;
import com.zriyo.aicodemother.model.entity.DeploymentHistory;
import com.zriyo.aicodemother.service.DeploymentHistoryService;
import com.zriyo.aicodemother.util.BeanCopyUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 应用部署历史记录 服务层实现。
 *
 */
@Service
@RequiredArgsConstructor
public class DeploymentHistoryServiceImpl extends ServiceImpl<DeploymentHistoryMapper, DeploymentHistory>  implements DeploymentHistoryService {
    private final AppMapper  appMapper;
    private final DeploymentHistoryMapper deploymentHistoryMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordDeployment(App app, String deployName, String initialVersion) {
        DeploymentHistory deploymentHistory = buildDeploymentHistory(app, app.getId(), deployName, initialVersion);
        deploymentHistoryMapper.insert(deploymentHistory);
        App appOne = UpdateEntity.of(App.class, app.getId());
        appOne.setLatestDeploymentId(deploymentHistory.getId());
        appOne.setIsPublished(1);
        appOne.setDeployedTime(app.getDeployedTime());
        appOne.setDeployKey(app.getDeployKey());
        appMapper.update(appOne);
    }

    @Override
    public List<DeploymentHistory> getDeploymentHistoryWithCursorPagination(Long id) {
        return deploymentHistoryMapper.getDeploymentHistoryWithCursorPagination(id);
    }


    public DeploymentHistory buildDeploymentHistory(App app, Long appId, String deployName,String initialVersion){
        DeploymentHistory deploymentHistory = BeanCopyUtil.copy(app, DeploymentHistory.class);
        deploymentHistory.setAppId(appId);
        deploymentHistory.setDeployName(deployName);
        deploymentHistory.setVersion(initialVersion);
        deploymentHistory.setDeployTime(app.getDeployedTime());
        return deploymentHistory;
    }
}
