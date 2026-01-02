package com.zriyo.aicodemother.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.zriyo.aicodemother.common.BaseResponse;
import com.zriyo.aicodemother.common.ResultUtils;
import com.zriyo.aicodemother.config.AppViewConfig;
import com.zriyo.aicodemother.exception.BusinessException;
import com.zriyo.aicodemother.exception.ErrorCode;
import com.zriyo.aicodemother.exception.ThrowUtils;
import com.zriyo.aicodemother.model.AppConstant;
import com.zriyo.aicodemother.model.dto.AppUpdateRequest;
import com.zriyo.aicodemother.model.dto.ChatCodeRequest;
import com.zriyo.aicodemother.model.dto.app.AppAddRequest;
import com.zriyo.aicodemother.model.dto.app.AppDeployRequest;
import com.zriyo.aicodemother.model.dto.app.AppQueryRequest;
import com.zriyo.aicodemother.model.dto.app.RollbackRequest;
import com.zriyo.aicodemother.model.entity.App;
import com.zriyo.aicodemother.model.entity.OptimizeRequest;
import com.zriyo.aicodemother.model.enums.CodeGenTypeEnum;
import com.zriyo.aicodemother.model.vo.AppCountVo;
import com.zriyo.aicodemother.model.vo.AppInfoVO;
import com.zriyo.aicodemother.model.vo.AppPageVO;
import com.zriyo.aicodemother.model.vo.AppVO;
import com.zriyo.aicodemother.service.AppService;
import com.zriyo.aicodemother.util.UserAuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 应用接口
 */
@RestController
@Validated
@RequestMapping("/app")
@RequiredArgsConstructor
@Slf4j
public class AppController {
    private final AppService appService;
    @Autowired
    private AppViewConfig appViewConfig;

    /**
     * 创建应用
     *
     * @param appAddRequest 应用创建请求
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addApp(@RequestBody @Valid AppAddRequest appAddRequest) {
        Long loginId = UserAuthUtil.getLoginId();
        Long appId = appService.createApp(appAddRequest, loginId);
        return ResultUtils.success(appId);
    }

    /**
     * 删除应用
     *
     * @param appId 应用 Id
     * @return
     */
    @PostMapping("/delete/{appId}")
    public BaseResponse<Long> deleteApp(@PathVariable Long appId) {
        Long loginId = UserAuthUtil.getLoginId();
        appService.deleteApp(appId, loginId);
        return ResultUtils.success(appId);
    }

    /**
     * 分页获取当前用户创建的应用列表
     *
     * @param appQueryRequest 查询请求
     * @param request         请求
     * @return 应用列表
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<AppVO>> listMyAppVOByPage(@RequestBody AppQueryRequest appQueryRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        Long loginId = UserAuthUtil.getLoginId();
        // 限制每页最多 20 个
        long pageSize = appQueryRequest.getPageSize();
        ThrowUtils.throwIf(pageSize > AppConstant.MAX_PAGE_SIZE, ErrorCode.PARAMS_ERROR, "每页最多查询 20 个应用");
        long pageNum = appQueryRequest.getPageNum();
        // 只查询当前用户的应用
        QueryWrapper queryWrapper = appService.getQueryWrapper(appQueryRequest, loginId);
        Page<App> appPage = appService.page(Page.of(pageNum, pageSize), queryWrapper);
        // 数据封装
        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        List<AppVO> appVOList = appService.getAppVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        return ResultUtils.success(appVOPage);
    }

    /**
     * 分页获取精选应用列表
     *
     * @param appQueryRequest 查询请求
     * @return 精选应用列表
     */
    @PostMapping("/good/list/page/vo")
    @SaIgnore
    public BaseResponse<Page<AppPageVO>> listGoodAppVOByPage(@RequestBody AppQueryRequest appQueryRequest) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 限制每页最多 20 个
        long pageSize = appQueryRequest.getPageSize();
        ThrowUtils.throwIf(pageSize > AppConstant.MAX_PAGE_SIZE, ErrorCode.PARAMS_ERROR, "每页最多查询 20 个应用");
        // 只查询精选的应用
        appQueryRequest.setPriority(AppConstant.GOOD_APP_PRIORITY);
        // 分页查询
        Page<AppPageVO> appPage = appService.getAppWithUserPage(appQueryRequest);
        return ResultUtils.success(appPage);
    }

    /**
     * 应用部署
     *
     * @param appDeployRequest 部署请求
     * @param request          请求
     * @return 部署 URL
     */
    @PostMapping("/deploy")
    public BaseResponse<String> deployApp(@RequestBody AppDeployRequest appDeployRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appDeployRequest == null, ErrorCode.PARAMS_ERROR);
        Long appId = appDeployRequest.getAppId();
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        // 获取当前登录用户
        Long userId = UserAuthUtil.getLoginId();
        // 调用服务部署应用
        String deployUrl = appService.deployApp(appId, userId, appDeployRequest.getDeployName());
        return ResultUtils.success(deployUrl);
    }

    private Path getAppOutputDir(Long appId) {
        if (appId == null || appId <= 0) {
            throw new IllegalArgumentException("Invalid appId");
        }
        String baseOutputPath = CodeGenTypeEnum.VUE_PROJECT.getValue();
        return com.zriyo.aicodemother.util.CodeOutputManager.getCodeOutputBaseDir()
                .resolve(baseOutputPath + "_" + appId)
                .resolve(AppConstant.BUILD_OUTPUT_DIR);
    }

    @GetMapping("/view/**")
    @SaIgnore
    public void previewApp(HttpServletRequest request,
                           HttpServletResponse response) throws IOException {

        // --- Token 处理结束 ---
        String uri = request.getRequestURI();
        String prefix = appViewConfig.getPrefix();
        if (!uri.startsWith(prefix)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String pathAfterView = uri.substring(prefix.length());
        String appIdStr;
        String relativePath;

        int firstSlash = pathAfterView.indexOf('/');
        if (firstSlash == -1 || firstSlash == pathAfterView.length() - 1) {
            appIdStr = pathAfterView.replaceAll("/$", "");
            relativePath = AppConstant.STATIC_ENTRY_FILE;
        } else {
            appIdStr = pathAfterView.substring(0, firstSlash);
            relativePath = pathAfterView.substring(firstSlash + 1);
        }

        // 校验 appId
        Long appId;
        try {
            appId = Long.valueOf(appIdStr);
            if (appId <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "无效的应用 ID");
            return;
        }

        appService.viewApp(appId, 0L);
        Path appDir = getAppOutputDir(appId);
        Path targetFile = appDir.resolve(relativePath).normalize();

        if (!targetFile.startsWith(appDir)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "非法路径访问");
            return;
        }

        if (!Files.exists(targetFile)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "资源不存在: " + relativePath);
            return;
        }

        if (AppConstant.STATIC_ENTRY_FILE.equals(relativePath) || "index.htm".equals(relativePath)) {
            response.setContentType("text/html;charset=utf-8");
            String html = Files.readString(targetFile, java.nio.charset.StandardCharsets.UTF_8);

            // 把所有 src="/... 和 href="/... 改成 src="./... 和 href="./...
            html = html.replaceAll("src=\"/([^\"/])", "src=\"./$1")
                    .replaceAll("href=\"/([^\"/])", "href=\"./$1");

            response.getWriter().write(html);
        } else {
            // 非 HTML 文件：正常返回
            String mimeType = URLConnection.guessContentTypeFromName(
                    targetFile.getFileName().toString()
            );
            response.setContentType(mimeType != null ? mimeType : "application/octet-stream");
            Files.copy(targetFile, response.getOutputStream());
            response.getOutputStream().flush();
        }
    }

    /**
     * 应用回滚
     *
     * @param request 请求
     * @return 响应
     */
    @PostMapping("/rollback")
    public BaseResponse<Object> rollbackToHistory(@RequestBody RollbackRequest request) {
        appService.rollbackToHistory(request);
        return ResultUtils.success();
    }

    /**
     * 应用聊天生成代码（流式 SSE）
     *
     * @return 生成结果流
     */
    @SaIgnore
    @PostMapping(value = "/chat/gen/code", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Object>> chatToGenCodeTest(@RequestBody ChatCodeRequest codeRequest) {
        Long userId = UserAuthUtil.loginByRequestToken();
        // 参数校验
        ThrowUtils.throwIf(codeRequest.getAppId() == null || codeRequest.getAppId() <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");
        // 调用服务生成代码（流式）
        return appService.NewChatToGenCode(codeRequest.getAppId(), codeRequest.getMessage(), userId, codeRequest.getFeedback());
    }

    /**
     * 取消当前对话
     */
    @PostMapping("/cancel")
    public BaseResponse<Boolean> cancelCurrentDialogue(Long appId) {
        return ResultUtils.success(appService.cancelCurrentDialogue(appId));
    }

    /**
     * 获取当前对话状态
     */
    @GetMapping("/status")
    public BaseResponse<Object> getAppStatus(Long appId) {
        return ResultUtils.success(appService.getAppStatus(appId));
    }

    /**
     * 获取 app 数量
     */
    @GetMapping("/count")
    public BaseResponse<AppCountVo> getAppCount() {
        return ResultUtils.success(appService.getAppCountVo(UserAuthUtil.getLoginId()));
    }

    /**
     * 修改 app 名字
     */
    @PostMapping("/update")
    public BaseResponse<Object> updateAppName(@RequestBody AppUpdateRequest request) {
        Long userId = UserAuthUtil.loginByRequestToken();
        appService.updateAppName(request, userId);
        return ResultUtils.success();
    }

    /**
     * 获取当前应用信息
     */
    @GetMapping("/info")
    public BaseResponse<AppInfoVO> getAppInfo(Long id) {
        Long userId = UserAuthUtil.getLoginId();
        return ResultUtils.success(appService.getAppInfoVO(id, userId));
    }

    /**
     * 优化用户提示词
     */
    @PostMapping(path = "/optimize", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @SaIgnore
    public Flux<ServerSentEvent<Object>> optimizePrompt(@RequestBody OptimizeRequest request) {
        Long userId = UserAuthUtil.getLoginId();
        return appService.optimizePrompt(request.getPrompt(), userId);
    }

    @GetMapping("/download")
    public ResponseEntity<InputStreamResource> downloadSourceCode(@RequestParam Long appId) throws IOException {
        // 🔒 1. 校验 appId 合法性
        if (appId == null || appId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "无效的应用ID");
        }
        // 🔒 2. 权限校验
        App app = appService.getApp(appId);
        if (app == null || !Objects.equals(app.getUserId(), UserAuthUtil.getLoginId())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "无权访问该应用");
        }

        // 🔒 3. 安全构建目录路径（避免路径穿越）
        String projectName = AppConstant.VUE_PROJECT_PREFIX + appId;
        // 确保 projectName 不包含路径分隔符（防御 ../ 注入）
        if (projectName.contains("/") || projectName.contains("\\") || projectName.contains("..")) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "非法项目名称");
        }

        Path baseDir = Paths.get(AppConstant.TMP_DIR, AppConstant.APP_GEN_FILE_PATH);
        Path sourceDir = baseDir.resolve(projectName).normalize();

        // 🔒 4. 防止路径逃逸：确保最终路径仍在 baseDir 下
        if (!sourceDir.startsWith(baseDir)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "非法目录访问");
        }

        if (!Files.exists(sourceDir)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "源码尚未生成，请稍后重试");
        }

        // 📦 5. 压缩为 ZIP（内存中）
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zipOut = new ZipOutputStream(baos)) {
            zipDirectory(sourceDir, projectName, zipOut);
        } // 自动 close zipOut

        byte[] zipBytes = baos.toByteArray();
        InputStreamResource resource = new InputStreamResource(new ByteArrayInputStream(zipBytes));

        // 📥 6. 设置响应头（安全文件名）
        String safeFilename = "source_" + appId + ".zip"; // 避免特殊字符
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDispositionFormData("attachment", safeFilename); // 自动处理编码
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentLength(zipBytes.length);

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(zipBytes.length)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    /**
     * 安全地压缩目录，排除 dist / node_modules
     */
    private void zipDirectory(Path dir, String baseName, ZipOutputStream zipOut) throws IOException {
        Files.walk(dir)
                .filter(path -> {
                    Path relPath = dir.relativize(path);
                    String relStr = relPath.toString().replace('\\', '/'); // 统一为 /

                    // 排除 dist 和 node_modules（根目录下）
                    return !(relStr.startsWith("dist/") || relStr.equals("dist") ||
                            relStr.startsWith("node_modules/") || relStr.equals("node_modules"));
                })
                .forEach(path -> {
                    try {
                        Path relPath = dir.relativize(path);
                        String entryName = baseName + "/" + relPath.toString().replace('\\', '/');

                        // 🔒 防 Zip Slip：确保 entryName 不以 ../ 开头
                        if (entryName.contains("..")) {
                            throw new RuntimeException("非法文件路径: " + entryName);
                        }

                        if (Files.isDirectory(path)) {
                            zipOut.putNextEntry(new ZipEntry(entryName + "/"));
                            zipOut.closeEntry();
                        } else {
                            zipOut.putNextEntry(new ZipEntry(entryName));
                            Files.copy(path, zipOut);
                            zipOut.closeEntry();
                        }
                    } catch (IOException e) {
                        throw new RuntimeException("压缩失败: " + path, e);
                    }
                });
    }

}
