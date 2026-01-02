package com.zriyo.aicodemother.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.anji.captcha.util.StringUtils;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.util.UpdateEntity;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.zriyo.aicodemother.ai.AiCodeGenTypeRoutingService;
import com.zriyo.aicodemother.ai.AiCodeGeneratorServiceV2;
import com.zriyo.aicodemother.ai.factory.AiCodeGeneratorServiceFactoryV2;
import com.zriyo.aicodemother.ai.service.AiCodeGenTypeRoutingServiceImpl;
import com.zriyo.aicodemother.core.AiCodeGeneratorFacade;
import com.zriyo.aicodemother.core.handler.AiContextHolder;
import com.zriyo.aicodemother.core.handler.HtmlCodeGenSseHandler;
import com.zriyo.aicodemother.core.handler.VueProjectSseHandler;
import com.zriyo.aicodemother.core.pipeline.CodeGenPipelineBuilder;
import com.zriyo.aicodemother.core.pipeline.GenerationContext;
import com.zriyo.aicodemother.exception.BusinessException;
import com.zriyo.aicodemother.exception.ErrorCode;
import com.zriyo.aicodemother.exception.ThrowUtils;
import com.zriyo.aicodemother.mapper.AppMapper;
import com.zriyo.aicodemother.mapper.DeploymentHistoryMapper;
import com.zriyo.aicodemother.model.AppConstant;
import com.zriyo.aicodemother.model.MonitorContext;
import com.zriyo.aicodemother.model.RedisConstants;
import com.zriyo.aicodemother.model.dto.AppUpdateRequest;
import com.zriyo.aicodemother.model.dto.RuntimeFeedbackDTO;
import com.zriyo.aicodemother.model.dto.app.AppAddRequest;
import com.zriyo.aicodemother.model.dto.app.AppQueryRequest;
import com.zriyo.aicodemother.model.dto.app.RollbackRequest;
import com.zriyo.aicodemother.model.dto.chat.ChatMessage;
import com.zriyo.aicodemother.model.entity.App;
import com.zriyo.aicodemother.model.entity.ChatHistory;
import com.zriyo.aicodemother.model.entity.DeploymentHistory;
import com.zriyo.aicodemother.model.enums.ChatHistoryMessageTypeEnum;
import com.zriyo.aicodemother.model.enums.CodeGenTypeEnum;
import com.zriyo.aicodemother.model.enums.PointsReasonEnum;
import com.zriyo.aicodemother.model.message.StreamMessageTypeEnum;
import com.zriyo.aicodemother.model.vo.*;
import com.zriyo.aicodemother.service.AppService;
import com.zriyo.aicodemother.service.ChatHistoryService;
import com.zriyo.aicodemother.service.DeploymentHistoryService;
import com.zriyo.aicodemother.service.PointsAdjustService;
import com.zriyo.aicodemother.util.*;
import dev.langchain4j.service.TokenStream;
import jodd.util.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppServiceImpl extends ServiceImpl<AppMapper, App> implements AppService {


    private final AiCodeGeneratorFacade aiCodeGeneratorFacade;
    private final AppMapper appMapper;
    private final DeploymentHistoryMapper deploymentHistoryMapper;
    private final DeploymentHistoryService deploymentHistoryService;
    private final ChatHistoryService chatHistoryService;
    private final HtmlCodeGenSseHandler htmlCodeGenSseHandler;
    private final AiCodeGenTypeRoutingService aiCodeGenTypeRoutingService;
    private final VueProjectSseHandler vueProjectSseHandler;
    private final CodeGenPipelineBuilder codeGenPipelineBuilder;
    private final AiCodeGeneratorServiceFactoryV2 aiCodeGeneratorServiceFactoryV2;
    protected final ApplicationEventPublisher publisher;
    private final AiCodeGenTypeRoutingServiceImpl aiCodeGenTypeRoutingServiceImpl;
    private final PointsAdjustService pointsAdjustService;
    // 存放正在运行的任务槽。Key 是 appId
    private static final Map<Long, Sinks.Many<ServerSentEvent<Object>>> taskSinks = new ConcurrentHashMap<>();


    @Override
    public Long createApp(AppAddRequest appAddRequest, Long loginId) {
        pointsAdjustService.validatePoints(loginId);
        String initPrompt = appAddRequest.getInitPrompt();
        App app = BeanCopyUtil.copy(appAddRequest, App.class);
        app.setInitPrompt(initPrompt);
        app.setUserId(loginId);
        MonitorContext monitorContext = MonitorContext.builder()
                .userId("0")
                .appId("0")
                .build();
        try {
            AiContextHolder.set(monitorContext);
            String AppName = appAddRequest.getAppName();
            if (StringUtil.isBlank(AppName)) {
                AppName = "应用名称生成中...";
            }
            app.setAppName(AppName);
            CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.VUE_PROJECT;
            app.setCodeGenType(codeGenTypeEnum.getValue());
            boolean save = this.save(app);
            ThrowUtils.throwIf(!save, ErrorCode.OPERATION_ERROR);
            VirtualThreadUtils.runAsync(() -> {
                String appName = aiCodeGenTypeRoutingService.routeCodeGenType(initPrompt).appName();
                Long id = app.getId();
                App updateApp = UpdateEntity.of(App.class, id);
                updateApp.setAppName(appName);
                appMapper.update(updateApp);
            });
        } finally {
            AiContextHolder.remove();
        }
        return app.getId();
    }

    @Override
    public void deleteApp(Long appId, Long loginId) {

        Boolean appStatus = (Boolean) getAppStatus(appId);
        if (appStatus != null && appStatus && taskSinks.containsKey(appId)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "应用正在运行中，请稍后再试");
        }
        RedisUtils.deleteObject(RedisConstants.AI_CODE_GEN_TASK_RUNNING + appId);
        // 删除数据库中应用记录
        QueryWrapper eq = new QueryWrapper()
                .eq(App::getId, appId)
                .eq(App::getUserId, loginId);
        boolean remove = this.remove(eq);
        ThrowUtils.throwIf(!remove, ErrorCode.OPERATION_ERROR);

        // 删除关联的聊天记录
        boolean removeChat = chatHistoryService.deleteByAppId(appId);
        ThrowUtils.throwIf(!removeChat, ErrorCode.OPERATION_ERROR);

        // 构建目标路径
        Path baseDir = Paths.get(AppConstant.TMP_DIR, AppConstant.APP_GEN_FILE_PATH)
                .toAbsolutePath().normalize();
        Path targetDir = baseDir.resolve(AppConstant.VUE_PROJECT_PREFIX + appId).normalize();

        // 安全检查，防止越界删除
        if (!targetDir.startsWith(baseDir)) {
            throw new IllegalArgumentException("非法删除路径: " + targetDir);
        }
        VirtualThreadUtils.runAsync(() -> {
            try {
                safeDeleteDir(targetDir);
            } catch (IOException e) {
                log.error("删除目录失败：" + e.getMessage());
            }
        });

    }

    /**
     * 安全删除目录，普通文件和目录会被删除，软链接目录只删除链接本身，不删除真实内容
     */
    private void safeDeleteDir(Path dir) throws IOException {
        if (!Files.exists(dir)) return;

        Files.walkFileTree(dir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file); // 删除普通文件
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                // 遇到目录软链接时，不递归进入
                if (Files.isSymbolicLink(dir)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                // 删除目录或软链接本身
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    @Override
    public AppWithLatestVersionVO getAppVO(Long id) {
        App app = this.getApp(id);
        if (app == null) {
            return null;
        }

        // 1. 获取 app 当前记录的最新部署
        Long recordedLatestId = app.getLatestDeploymentId();
        DeploymentHistory recordedDeployment = null;
        if (recordedLatestId != null) {
            recordedDeployment = deploymentHistoryMapper.selectOneById(recordedLatestId);
        }

        // 2. 查询数据库中该应用真正的最新部署（按 deploy_time 最新）
        DeploymentHistory actualLatest = deploymentHistoryMapper.selectLatestByAppId(app.getId());

        // 3. 构造 VO
        AppWithLatestVersionVO vo = new AppWithLatestVersionVO();
        BeanUtil.copyProperties(app, vo);

        // 设置版本名称（优先用 recordedDeployment，若为空则用 actualLatest）
        if (recordedDeployment != null) {
            vo.setLatestVersion(recordedDeployment.getDeployName());
        } else if (actualLatest != null) {
            vo.setLatestVersion(actualLatest.getDeployName());
        }

        // 4. 判断：app 表中记录的 latestDeploymentId 是否等于实际最新的部署 ID
        boolean isLatestVersion = false;
        if (actualLatest != null && recordedLatestId != null) {
            if (recordedDeployment != null) {
                isLatestVersion = actualLatest.getVersion().equals(recordedDeployment.getVersion());
            }
        } else if (actualLatest == null && recordedLatestId == null) {
            // 两者都为空，也算一致（无部署）
            isLatestVersion = true;
        }
        // 否则：有实际部署但 app 没记录，或反之 → 不是最新的
        vo.setIsLatestVersion(isLatestVersion);

        return vo;
    }


    @Override
    public App getApp(Long id) {
        App byId = this.getById(id);
        if (byId == null) {
            return null;
        }
        return byId;
    }

    @Override
    public QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest, Long loginId) {
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }

        String appName = appQueryRequest.getAppName();
        String sortField = appQueryRequest.getSortField();
        String sortOrder = appQueryRequest.getSortOrder();
        Integer isPublish = appQueryRequest.getIsPublished(); // 可能为 null

        QueryWrapper wrapper = QueryWrapper.create()
                .like("appName", appName)
                .eq("userId", loginId);

        if ( isPublish != null && isPublish >= 0) {
            wrapper.eq(App::getIsPublished, isPublish);
        }

        // 处理排序（同样建议对 sortField 做安全校验）
        if (StringUtils.isNotBlank(sortField)) {
            boolean isAsc = "ascend".equals(sortOrder);
            wrapper.orderBy(sortField, isAsc);
        }

        return wrapper;
    }

    @Override
    public List<AppPageVO> getAppPageVOList(List<App> records) {
        return BeanCopyUtil.copyList(records, AppPageVO.class);
    }

    @Override
    public List<AppVO> getAppVOList(List<App> records) {
        return BeanCopyUtil.copyList(records, AppVO.class);
    }

    @Override
    public Page<AppPageVO> getAppWithUserPage(AppQueryRequest request) {
        long pageNum = request.getPageNum();
        long pageSize = Math.min(request.getPageSize(), 20);
        long offset = (pageNum - 1) * pageSize;

        Integer priority = AppConstant.GOOD_APP_PRIORITY;
        String appName = request.getAppName();

        // 1. 查数据
        List<AppPageVO> records = appMapper.selectAppWithUserList(priority, appName, offset, pageSize);

        // 2. 查总数
        long total = appMapper.countAppWithUser(priority, appName);

        // 3. 封装分页对象
        Page<AppPageVO> page = new Page<>();
        page.setPageNumber(pageNum);
        page.setPageSize(pageSize);
        page.setTotalRow(total);
        page.setRecords(records);
        page.setTotalPage(total);
        return page;
    }

    @Override
    public Flux<ServerSentEvent<Object>> chatToGenCode(Long appId, String message, Long userId) {
        App app = getApp(appId, userId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        String codeGenType = app.getCodeGenType();
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用生成类型异常");
        }
        ChatMessage chatMessage = getChatMessage(appId, message, ChatHistoryMessageTypeEnum.USER);
        Flux<String> source = aiCodeGeneratorFacade
                .generateAndSaveCodeStream(message, codeGenTypeEnum, appId)
                .share(); // 单订阅即可触发

        Flux<ServerSentEvent<Object>> sseFlux = null;

        if (codeGenTypeEnum == CodeGenTypeEnum.HTML || codeGenTypeEnum == CodeGenTypeEnum.MULTI_FILE) {
            // HTML 或多文件类型，需要旁路收集完整内容
            sseFlux = htmlCodeGenSseHandler.handleStream(source, appId, codeGenType, userId, chatMessage);
        } else if (codeGenTypeEnum == CodeGenTypeEnum.VUE_PROJECT) {
            // Vue 项目类型，只用增量入库，不需要旁路
            sseFlux = vueProjectSseHandler.handleStream(source, appId, codeGenType, userId, chatMessage);
        }
        return sseFlux;
    }


    private ChatMessage getChatMessage(Long appId, String message, ChatHistoryMessageTypeEnum messageType) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setAppId(appId);
        chatMessage.setMessage(message);
        chatMessage.setMessageType(messageType.getValue());
        return chatMessage;
    }

    private App getApp(Long appId, Long userId) {
        QueryWrapper eq = new QueryWrapper().eq(App::getId, appId).eq(App::getUserId, userId);
        return this.getOne(eq);
    }

    @Override
    public String deployApp(Long appId, Long userId, String deployName) {
        App app = getApp(appId, userId);
        if (app == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        String deployKey = app.getDeployKey();
        if (StringUtil.isBlank(deployKey)) {
            int salt = 0;
            while (true) {
                String Key = encodeWithSalt(appId, salt);
                QueryWrapper eq = new QueryWrapper()
                        .eq(App::getDeployKey, Key);
                List<App> apps = appMapper.selectListByQuery(eq);
                if (apps.isEmpty()) {
                    deployKey = Key;
                    break;
                }
                salt++;
            }
        }
        String codeGenType = app.getCodeGenType();
        Long id = app.getId();
        String deployUrl = codeGenType + "_" + id;
        //获取部署时间
        LocalDateTime deployTime = LocalDateTime.now();
        app.setDeployedTime(deployTime);
        app.setDeployKey(deployKey);
        app.setIsPublished(1);
        String initialVersion = getInitialVersion(app);

        if (app.getCodeGenType().equals(CodeGenTypeEnum.VUE_PROJECT.getValue())) {
            String finalDeployKey = deployKey;
            VirtualThreadUtils.runAsync(() -> {
                String filePath = CodeGenTypeEnum.VUE_PROJECT.getValue() + "_" + id;
                //静件部署
                CodeOutputManager.copyHtmlDirToDeploy(filePath, finalDeployKey, null);
                CodeOutputManager.archiveAppVersion(finalDeployKey, initialVersion);
            });
        } else if (app.getCodeGenType().equals(CodeGenTypeEnum.HTML.getValue())) {
            CodeOutputManager.copyHtmlDirToDeploy(deployUrl, deployKey, null);
            CodeOutputManager.archiveAppVersion(deployKey, initialVersion);
        }

        try {
            deploymentHistoryService.recordDeployment(app, deployName, initialVersion);
        } catch (
                Exception e) {
            log.error("保存部署历史记录失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }

        String url = "/" + deployKey;
        return url;
    }

    @Override
    public void rollbackToHistory(RollbackRequest request) {
        Long appId = request.getAppId();
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(app.getDeployKey() == null, ErrorCode.NOT_FOUND_ERROR);
        Long rollbackId = request.getRollbackId();
        DeploymentHistory deploymentHistory = deploymentHistoryMapper
                .selectOneByQuery(new QueryWrapper()
                        .eq(DeploymentHistory::getAppId, appId)
                        .eq(DeploymentHistory::getId, rollbackId));
        ThrowUtils.throwIf(deploymentHistory == null, ErrorCode.NOT_FOUND_ERROR);
        CodeOutputManager.deployFromHistory(app.getDeployKey(), deploymentHistory.getVersion());
        app.setLatestDeploymentId(rollbackId);
        int update = appMapper.update(app);
        ThrowUtils.throwIf(update <= 0, ErrorCode.SYSTEM_ERROR);
    }

    @Override
    public Flux<ServerSentEvent<Object>> NewChatToGenCode(Long appId, String message, Long userId, RuntimeFeedbackDTO feedback) {
        // --- 1. 重连逻辑：检查内存中是否已有运行中的任务 ---
        if (taskSinks.containsKey(appId)) {
            log.info("🔗 [重连] 检测到 appId: {} 任务正在运行，接入实时流并同步历史进度", appId);
            // 使用 all() 的 Sink 会在这里自动把之前缓存的所有消息喷发给前端
            return taskSinks.get(appId).asFlux().mergeWith(createPingFlux());
        }
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "用户消息不能为空");
        // --- 2. 参数校验与基础数据准备 ---
        App app = getApp(appId, userId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);

        String codeGenType = app.getCodeGenType();
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
        if (codeGenTypeEnum == null) {
            return Flux.just(SseEventBuilder.of(StreamMessageTypeEnum.ERROR, "应用生成类型异常"));
        }

        // --- 3. 任务锁判定 (防止并发) ---
        Boolean taskRunning = RedisUtils.getCacheObject(RedisConstants.AI_CODE_GEN_TASK_RUNNING + appId);
        if (Boolean.TRUE.equals(taskRunning)) {
            // 内存没 Sink 但 Redis 有锁，说明是集群其它节点任务或异常残留
            throw new BusinessException(ErrorCode.EXECUTING);
        }

        // --- 4. 业务初始化 ---
        // 判定是否为首次构建
        ChatHistory skeletonRecord = chatHistoryService.getOne(new QueryWrapper()
                .eq(ChatHistory::getAppId, appId)
                .eq(ChatHistory::getMessageType, ChatHistoryMessageTypeEnum.SKELETON.getValue()));
        boolean isFirstBuild = Objects.isNull(skeletonRecord);

        GenerationContext context = new GenerationContext();
        context.setAppId(appId);
        context.setUserId(userId);
        context.setMessage(message);
        context.setRuntimeFeedback(feedback);
        context.setCodeGenType(codeGenTypeEnum);
        context.setIsOosUrl(StrUtil.isNotBlank(app.getCover()));
        context.setIsFirstBuild(isFirstBuild);

        if (context.getIsFirstBuild()) {
            pointsAdjustService.adjustPoints(userId, PointsReasonEnum.CHAT_CONSUME, appId,null);
        } else {
            pointsAdjustService.adjustPoints(userId, PointsReasonEnum.APP_GENERATE, appId,null);
        }

        // --- 5. 创建消息分发中心 (Sink) ---
        Sinks.Many<ServerSentEvent<Object>> sink = Sinks.many().replay().all();
        taskSinks.put(appId, sink);
        RedisUtils.setCacheObject(RedisConstants.AI_CODE_GEN_TASK_RUNNING + appId, true);

        // --- 6. 异步开启 Pipeline 任务 (不随 HTTP 连接断开而停止) ---
        Flux.defer(() -> {
                    AiContextHolder.set(MonitorContext.builder()
                            .appId(String.valueOf(appId))
                            .userId(String.valueOf(userId))
                            .build());

                    // A. 异步优化提示词 & 发送 SSE 状态
                    Flux<ServerSentEvent<Object>> optimizationFlux = Flux.empty();
                    if (context.getIsFirstBuild() && context.getRuntimeFeedback() == null) {
                        optimizationFlux = Flux.concat(
                                Flux.just(SseEventBuilder.of(StreamMessageTypeEnum.AI_RESPONSE, "正在利用 AI 智能优化你的需求描述...\n")),
                                Mono.fromCallable(() -> {
                                            try {
                                                String optimized = aiCodeGenTypeRoutingServiceImpl.optimizeUserPrompt(context.getMessage());
                                                if (StrUtil.isNotBlank(optimized)) {
                                                    context.setMessage(optimized);
                                                }
                                            } catch (Exception e) {
                                                log.warn("提示词异步优化失败: {}", e.getMessage());
                                            }
                                            return true;
                                        }).subscribeOn(Schedulers.boundedElastic())
                                        .flatMapMany(res -> Flux.just(SseEventBuilder.of(StreamMessageTypeEnum.TOOL_EXECUTED, "需求优化完毕，正在进入生成环节...")))
                        );
                    }

                    // B. 异步保存优化后的消息入库
                    Mono<Void> saveMessageMono = Mono.fromRunnable(() -> {
                        ChatMessage userChatMessage = new ChatMessage();
                        userChatMessage.setAppId(appId);
                        userChatMessage.setMessage(context.getMessage());
                        userChatMessage.setMessageType(ChatHistoryMessageTypeEnum.USER.getValue());
                        userChatMessage.setUserVisible(1);
                        chatHistoryService.addChatMessage(userChatMessage, userId);
                        context.setMessageId(userChatMessage.getId());
                    }).subscribeOn(Schedulers.boundedElastic()).then();

                    // C. 串联执行
                    return Flux.concat(
                            optimizationFlux,
                            saveMessageMono.thenMany(codeGenPipelineBuilder.buildChain().handle(context))
                    );
                })
                .subscribeOn(Schedulers.boundedElastic()) // 在 IO 密集型线程池运行
                .doOnNext(event -> {
                    // 将 Pipeline 产生的每一条消息丢进 Sink
                    sink.tryEmitNext(event);
                })
                .doOnError(throwable -> {
                    log.error("代码生成过程中发生错误: appId={}", appId, throwable);
                    sink.tryEmitNext(SseEventBuilder.of(StreamMessageTypeEnum.ERROR, "系统内部错误: " + throwable.getMessage()));
                })
                .doFinally(signalType -> {
                    AiContextHolder.remove();
                    RedisUtils.deleteObject(RedisConstants.AI_CODE_GEN_TASK_RUNNING + appId);
                    taskSinks.remove(appId);
                    sink.tryEmitComplete();
                    log.info("AI 代码生成任务完全终结，资源清理完毕: appId={}, signal={}", appId, signalType);
                })
                .subscribe();

        // --- 7. 返回 Sink 的流给当前 HTTP 连接 ---
        // 使用 takeUntilOther 确保心跳在 Sink 关闭时同步停止
        return sink.asFlux().mergeWith(createPingFlux().takeUntilOther(sink.asFlux().then()));
    }

    /**
     * 创建心跳流
     */
    private Flux<ServerSentEvent<Object>> createPingFlux() {
        return Flux.interval(Duration.ofSeconds(15))
                .map(tick -> SseEventBuilder.of(StreamMessageTypeEnum.PING, "ping"));
    }

    @Override
    public void viewApp(Long appId, Long loginId) {
        App app = appMapper.selectOneById(appId);
        if (Objects.isNull(app)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
    }

    @Override
    public AppCountVo getAppCountVo(Long loginId) {
        AppCountVo appCountVo = new AppCountVo();
        appCountVo.setMaxCount(20);
        long count = this.count(new QueryWrapper().eq(App::getUserId, loginId));
        appCountVo.setCurrentCount((int) count);
        return appCountVo;
    }

    @Override
    public void updateOssUrl(Long appId, String oosUrl) {
        try {
            App app = UpdateEntity.of(App.class, appId);
            app.setCover(oosUrl);
            appMapper.update(app);
        } catch (Exception e) {
            log.error("更新应用封面失败", e);
        }
    }

    @Override
    public Boolean cancelCurrentDialogue(Long appId) {
        App app = appMapper.selectOneById(appId);
        if (Objects.isNull(app) || StringUtil.isBlank(app.getCover())) {
            return false;
        }
        RedisUtils.setCacheObject(RedisConstants.AI_CODE_GEN_TASK_RUNNING + appId, false);
        return true;
    }

    @Override
    public Object getAppStatus(Long appId) {
        return RedisUtils.getCacheObject(RedisConstants.AI_CODE_GEN_TASK_RUNNING + appId);
    }

    @Override
    public void updateAppName(AppUpdateRequest request, Long userId) {
        // 1. 校验 ID 是否存在
        Long appId = request.getAppId();
        if (appId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        }

        // 2. 查询并验证权限
        App app = appMapper.selectOneById(appId);
        if (app == null || !app.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }

        // 3. 更新字段（建议增加参数合法性判断）
        if (StringUtils.isNotBlank(request.getAppName())) {
            app.setAppName(request.getAppName());
        }

        if (Boolean.TRUE.equals(request.getIsOffline())) {
            app.setIsPublished(0);
        }

        // 5. 执行更新
        appMapper.update(app);
    }


    @Override
    public AppInfoVO getAppInfoVO(Long appId, Long userId) {
        App app = appMapper.selectOneById(appId);
        if (Objects.isNull(app) || !app.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        AppInfoVO copy = BeanCopyUtil.copy(app, AppInfoVO.class);
        copy.setIsPublished(app.getIsPublished());
        return copy;
    }

    @Override
    public Flux<ServerSentEvent<Object>> optimizePrompt(String prompt, Long userId) {
        if (StringUtil.isBlank(prompt) && prompt.length() < 10) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请输入有效的提示词");
        }
        return Flux.create(sink -> {
            try {
                MonitorContext monitorContext = MonitorContext.builder()
                        .userId("0")
                        .appId("0")
                        .build();
                AiContextHolder.set(monitorContext);
                AiCodeGeneratorServiceV2 aiCodeService = aiCodeGeneratorServiceFactoryV2.getAiCodeService(userId);
                TokenStream tokenStream = aiCodeService.optimizePromptTokenStream(prompt);
                tokenStream.onPartialResponse(content -> {
                            // 即使不处理文本内容，也必须显式配置此监听器
                            log.debug("AI Text Stream: {}", content);
                            sink.next(SseEventBuilder.of(StreamMessageTypeEnum.AI_RESPONSE, content));
                        })// 响应完成后：发送一个结束信号并关闭 Flux 流
                        .onCompleteResponse(response -> {
                            log.info("AI 响应生成完毕");
                            // 可选：发送一个特殊的 DONE 消息告知前端结束
                            sink.next(SseEventBuilder.of(StreamMessageTypeEnum.AI_DONE));
                            sink.complete();

                        })
                        // 异常处理：将错误传递给下游
                        .onError(error -> {
                            log.error("AI 流式生成发生异常", error);
                            sink.error(error);
                        });
                tokenStream.start();
            } catch (Exception e) {
                log.error("AI 流式生成发生异常", e);
            } finally {
                AiContextHolder.remove();
            }

        });
    }


    private String getInitialVersion(App app) {
        //获取自增版本号
        String initialVersion = null;
        DeploymentHistory latestHistory = deploymentHistoryMapper.selectOneNewData(app.getId());
        if (latestHistory == null) {
            initialVersion = AtomicVersionGenerator.INITIAL_VERSION;
        } else {
            initialVersion = getVersion(latestHistory);
        }

        return initialVersion;
    }

    private String getVersion(DeploymentHistory latestHistory) {
        String initialVersion;
        AtomicVersionGenerator generator = new AtomicVersionGenerator(latestHistory.getVersion());
        generator.nextVersion();
        initialVersion = generator.currentVersion();
        return initialVersion;
    }

    public String encodeWithSalt(long value, int salt) {
        long newValue = value + salt; // 用 salt 作为扰动
        return encode(newValue);
    }

    private String encode(long value) {
        char[] ALPHABET =
                "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
        int BASE = ALPHABET.length;
        StringBuilder sb = new StringBuilder();
        while (value > 0) {
            int idx = (int) (value % BASE);
            sb.append(ALPHABET[idx]);
            value = value / BASE;
        }
        return sb.reverse().toString();
    }

    @Override
    public Map<Long, Long> countAppByUserIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        List<Map<String, Object>> appCounts = appMapper.countAppByUserIds(userIds);
        return appCounts.stream()
                .collect(Collectors.toMap(
                        m -> ((Number) m.get("userId")).longValue(),
                        m -> ((Number) m.get("appCount")).longValue()
                ));
    }
}
