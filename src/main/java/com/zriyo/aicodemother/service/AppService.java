package com.zriyo.aicodemother.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.zriyo.aicodemother.model.dto.AppUpdateRequest;
import com.zriyo.aicodemother.model.dto.RuntimeFeedbackDTO;
import com.zriyo.aicodemother.model.dto.app.AppAddRequest;
import com.zriyo.aicodemother.model.dto.app.AppQueryRequest;
import com.zriyo.aicodemother.model.dto.app.RollbackRequest;
import com.zriyo.aicodemother.model.entity.App;
import com.zriyo.aicodemother.model.vo.*;
import jakarta.validation.Valid;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

public interface AppService  extends IService<App> {
    Long createApp(@Valid AppAddRequest appAddRequest, Long loginId);

    void deleteApp(Long appId, Long loginId);

    AppWithLatestVersionVO getAppVO(Long app);



    App getApp(Long id);

    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest, Long loginId);

    List<AppPageVO> getAppPageVOList(List<App> records);

    List<AppVO> getAppVOList(List<App> records);

    Page<AppPageVO> getAppWithUserPage(AppQueryRequest request);

    Flux<ServerSentEvent<Object>> chatToGenCode(Long appId, String  message, Long userId);

    String deployApp(Long appId, Long userId,  String deployName);

    void rollbackToHistory(RollbackRequest request);

    Flux<ServerSentEvent<Object>> NewChatToGenCode(Long appId, String message, Long userId, RuntimeFeedbackDTO feedback);

    void viewApp(Long appId, Long loginId);

    AppCountVo getAppCountVo(Long loginId);

    void updateOssUrl(Long appId, String oosUrl);


    Boolean cancelCurrentDialogue(Long appId);

    Object getAppStatus(Long appId);

    void updateAppName(AppUpdateRequest request, Long userId);

    AppInfoVO getAppInfoVO(Long appId, Long userId);

    Flux<ServerSentEvent<Object>> optimizePrompt(String prompt, Long userId);

    /**
     * 批量统计用户的应用数量
     * @param userIds 用户 ID 列表
     * @return Map<userId, appCount>
     */
    Map<Long, Long> countAppByUserIds(List<Long> userIds);
}
