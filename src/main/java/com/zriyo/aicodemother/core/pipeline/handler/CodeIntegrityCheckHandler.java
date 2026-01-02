package com.zriyo.aicodemother.core.pipeline.handler;

import cn.hutool.core.io.FileUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.WaitUntilState;
import com.zriyo.aicodemother.ai.AiCodeGeneratorServiceV2;
import com.zriyo.aicodemother.ai.factory.AiCodeGeneratorServiceFactoryV2;
import com.zriyo.aicodemother.ai.service.AiCodeGenTypeRoutingServiceImpl;
import com.zriyo.aicodemother.core.handler.AiContextHolder;
import com.zriyo.aicodemother.core.pipeline.GenerationContext;
import com.zriyo.aicodemother.core.pipeline.service.CodeGenRecordService;
import com.zriyo.aicodemother.model.AppConstant;
import com.zriyo.aicodemother.model.MonitorContext;
import com.zriyo.aicodemother.model.dto.ProjectSkeletonDTO;
import com.zriyo.aicodemother.model.enums.AiCodeGenStage;
import com.zriyo.aicodemother.model.enums.ToolAction;
import com.zriyo.aicodemother.model.message.StreamMessageTypeEnum;
import com.zriyo.aicodemother.oos.FileStorageService;
import com.zriyo.aicodemother.service.AiToolLogService;
import com.zriyo.aicodemother.service.ChatHistoryService;
import com.zriyo.aicodemother.util.ProjectDoctor;
import com.zriyo.aicodemother.util.SseEventBuilder;
import com.zriyo.aicodemother.util.StaticFileServer;
import dev.langchain4j.service.TokenStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.annotation.Order;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.function.Supplier;

/**
 * 代码完整性检查与自动修复处理器
 * 职责：执行静态/动态诊断，驱动 AI 修复 Bug，并捕获预览截图。
 */
@Component
@Order(6)
@Slf4j
public class CodeIntegrityCheckHandler extends AbstractCodeGenHandler {

    private final AiCodeGeneratorServiceFactoryV2 aiCodeGeneratorServiceFactory;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final FileStorageService fileStorageService;
    private static final int MAX_RETRY = AppConstant.AUTO_FIX_MAX_RETRY;
    private static final String BUCKET_LOGICAL_NAME = "documents";

    public CodeIntegrityCheckHandler(AiCodeGeneratorServiceFactoryV2 aiCodeGeneratorServiceFactory,
                                     CodeGenRecordService codeGenRecordService,
                                     ChatHistoryService chatHistoryService,
                                     AiToolLogService aiToolLogService,
                                     FileStorageService fileStorageService,
                                     ApplicationEventPublisher publisher,
                                     AiCodeGenTypeRoutingServiceImpl aiCodeGenTypeRoutingService) {
        super(codeGenRecordService, chatHistoryService, aiToolLogService, publisher, aiCodeGenTypeRoutingService);
        this.aiCodeGeneratorServiceFactory = aiCodeGeneratorServiceFactory;
        this.fileStorageService = fileStorageService;
    }

    @Override
    protected AiCodeGenStage getStage() {
        return AiCodeGenStage.DIAGNOSIS;
    }

    @Override
    protected Flux<ServerSentEvent<Object>> doExecute(GenerationContext context) {
        Long appId = context.getAppId();
        String projectDirName = AppConstant.VUE_PROJECT_PREFIX + appId;
        String projectRoot = Paths.get(ProjectDoctor.TMP_CODE_OUTPUT, projectDirName).toString();

        if (stopGeneration(context)) return stopMessage();

        return Flux.<ServerSentEvent<Object>>create(emitter -> {
            AiContextHolder.set(MonitorContext.builder().appId(String.valueOf(appId)).userId(String.valueOf(context.getUserId())).build());

            String httpUrl = null;
            try {
                emitter.next(SseEventBuilder.of(StreamMessageTypeEnum.DIAGNOSIS_PROCESS, "正在初始化诊断流水线..."));
                emitter.next(SseEventBuilder.of(StreamMessageTypeEnum.DIAGNOSIS_PROCESS, "项目构建检查中..."));

                // 1. 静态构建诊断循环 (修复语法、依赖、文件缺失)
                boolean buildPass = runCheckLoop(emitter, context, projectRoot, projectDirName, "构建编译",
                        () -> { cleanDist(projectRoot); return ProjectDoctor.runStaticDiagnosis(projectRoot); }, null);

                if (buildPass) {
                    Path distPath = Paths.get(projectRoot, AppConstant.BUILD_OUTPUT_DIR);
                    httpUrl = StaticFileServer.start(distPath.toString());
                    processScreenshot(emitter, context, httpUrl, projectDirName);
                    emitter.next(SseEventBuilder.of(StreamMessageTypeEnum.DIAGNOSIS_SUCCESS, "项目构建成功，预览已生成！"));
                } else {
                    emitter.next(SseEventBuilder.of(StreamMessageTypeEnum.DIAGNOSIS_ERROR, "构建多次尝试失败，请检查语法或依赖配置。"));
                    if (context != null) context.setIsError(true);
                }

            } catch (Exception e) {
                log.error("诊断流水线致命异常: ", e);
                if (context != null) context.setIsError(true);
                emitter.next(SseEventBuilder.of(StreamMessageTypeEnum.SYSTEM_ERROR, "系统故障: " + e.getMessage()));
            } finally {
                if (httpUrl != null) StaticFileServer.stop(httpUrl);
                emitter.complete();
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private boolean runCheckLoop(FluxSink<ServerSentEvent<Object>> emitter, GenerationContext context,
                                 String projectRoot, String projectDirName, String stageName,
                                 Supplier<ProjectDoctor.DiagnosisResult> checkSupplier, Runnable postFixAction) {

        for (int i = 0; i <= MAX_RETRY; i++) {
            ProjectDoctor.DiagnosisResult result = checkSupplier.get();
            if (result.success) return true;

            if (i < MAX_RETRY) {
                emitter.next(SseEventBuilder.of(StreamMessageTypeEnum.DIAGNOSIS_PROCESS, String.format("检测到 %s 异常，正在请求 AI 修复 ... ", stageName)));
                boolean fixResult = performSynchronousAiFix(emitter, context, projectRoot, projectDirName, result, i);
                if (!fixResult) continue;
                if (postFixAction != null) postFixAction.run();
            }
        }
        return false;
    }

    private boolean performSynchronousAiFix(FluxSink<ServerSentEvent<Object>> emitter, GenerationContext context,
                                            String projectRoot, String projectDirName, ProjectDoctor.DiagnosisResult result, int retryCount) {
        String prompt = buildFixPrompt(projectRoot, result, context.getSkeleton(), retryCount);
        if (prompt == null) return false;

        String errorFile = result.errorFilePath != null ? result.errorFilePath : "src/App.vue";
        String toolBindPath = projectDirName + "/" + errorFile;

        // 强制失效 Service 以清除幻觉上下文
        aiCodeGeneratorServiceFactory.invalidateService(toolBindPath, context.getCodeGenType());
        AiCodeGeneratorServiceV2 aiService = aiCodeGeneratorServiceFactory.getAiErrorCodeGeneratorService(
                toolBindPath, context.getCodeGenType(), projectDirName, context.getAppId());

        try {
            // 同步等待 AI 修复流结束
            handleAiFixStream(aiService, prompt, context, errorFile).toIterable().forEach(emitter::next);
            return true;
        } catch (Exception e) {
            log.error("AI 修复执行失败: ", e);
            return false;
        }
    }

    private Flux<ServerSentEvent<Object>> handleAiFixStream(AiCodeGeneratorServiceV2 aiService, String prompt, GenerationContext context, String filePath) {
        return Flux.create(sink -> {
            AiContextHolder.set(MonitorContext.builder().appId(String.valueOf(context.getAppId())).userId(String.valueOf(context.getUserId())).build());
            long startTime = System.currentTimeMillis();

            TokenStream tokenStream = super.invokeTokenStream(aiService, context, prompt, FIX_BUG);
            java.util.concurrent.atomic.AtomicBoolean toolExecuted = new java.util.concurrent.atomic.AtomicBoolean(false);

            tokenStream
                    .onPartialResponse(content -> {
                        log.debug("AI 修复建议文本: {}", content);
                    })
                    .onPartialToolExecutionRequest((index, req) -> toolExecuted.set(true))
                    .onToolExecuted(exec -> {
                        savaToolLog(context, filePath, exec, startTime, ToolAction.FIX_BUG);
                        sink.next(SseEventBuilder.of(StreamMessageTypeEnum.CODE_TOOL_EXECUTED, "补丁已自动应用"));
                    })
                    .onCompleteResponse(resp -> {
                        String aiText = resp.aiMessage() != null ? resp.aiMessage().text() : "";
                        if (!toolExecuted.get() && aiText != null && aiText.trim().length() > 5) {
                            sink.error(new IllegalStateException("AI 未调用工具修复代码"));
                            return;
                        }
                        sink.next(SseEventBuilder.of(StreamMessageTypeEnum.TOOL_DONE));
                        sink.complete();
                    })
                    .onError(sink::error)
                    .start();
        });
    }

    private void processScreenshot(FluxSink<ServerSentEvent<Object>> emitter, GenerationContext context, String httpUrl, String projectDirName) {
        File screenshot = null;
        try {
            emitter.next(SseEventBuilder.of(StreamMessageTypeEnum.DIAGNOSIS_PROCESS, "正在生成应用预览截图..."));
            screenshot = captureScreenshot(httpUrl, projectDirName);
            if (screenshot != null && screenshot.exists() && !context.getIsOosUrl()) {
                String ossUrl = fileStorageService.uploadFile(BUCKET_LOGICAL_NAME, new FileToMultipartFile(screenshot));
                context.setOosUrl(ossUrl);
                context.setIsOosUrl(true);
                emitter.next(SseEventBuilder.of(StreamMessageTypeEnum.TOOL_EXECUTED, ossUrl));
            }
        } catch (Exception e) {
            log.error("预览截图流程失败", e);
        } finally {
            if (screenshot != null && screenshot.exists()) screenshot.delete();
        }
    }

    private File captureScreenshot(String targetUrl, String projectDirName) {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setArgs(Arrays.asList(
                    "--no-sandbox", "--disable-setuid-sandbox", "--disable-dev-shm-usage", "--disable-gpu"
            )));
            Page page = browser.newPage();
            page.navigate(targetUrl, new Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE).setTimeout(60000));
            page.waitForSelector("#app", new Page.WaitForSelectorOptions().setTimeout(20000));

            Path destPath = Paths.get(System.getProperty("java.io.tmpdir"), "snap_" + System.currentTimeMillis() + ".png");
            page.screenshot(new Page.ScreenshotOptions().setPath(destPath).setFullPage(true));
            return destPath.toFile();
        } catch (Exception e) {
            log.error("Playwright 截图失败: {}", e.getMessage());
            return null;
        }
    }

    private String buildFixPrompt(String projectRoot, ProjectDoctor.DiagnosisResult result, ProjectSkeletonDTO skeleton, int retryCount) {
        try {
            String errorPath = result.errorFilePath;
            if (errorPath != null && (errorPath.contains("dist/") || errorPath.contains("index-"))) errorPath = null;

            Path filePath = errorPath != null ? Paths.get(projectRoot, errorPath) : null;
            String sourceCode = (filePath != null && Files.exists(filePath)) ? Files.readString(filePath) : "【无法定位源码，请根据报错重构】";

            ObjectNode globalNode = objectMapper.createObjectNode();
            globalNode.set("styleGuide", objectMapper.valueToTree(skeleton.getGlobal().getStyleGuide()));

            return "\n\n# 📋 Diagnosis Context\n" +
                    "- Error Phase: " + result.phase + "\n" +
                    "- Error File: " + result.errorFilePath + "\n" +
                    "- Error Log: " + result.message + "\n\n" +
                    "# 📄 Current Source Code:\n```vue\n" + sourceCode + "\n```\n\n" +
                    "==============================================================\n" +
                    "⚠️ FINAL PROTOCOL (MACHINE ONLY):\n" +
                    "1. Action: Call `writeFile` with the full FIXED code now.\n" +
                    "2. Sequence: `writeFile` -> `finishRepair`. STOP generating after.\n" +
                    "3. Zero Tolerance: NO text, NO markdown, NO explanations.\n" +
                    "==============================================================";
        } catch (Exception e) { return null; }
    }

    private void cleanDist(String projectRoot) {
        File dist = Paths.get(projectRoot, AppConstant.BUILD_OUTPUT_DIR).toFile();
        if (dist.exists()) FileUtil.del(dist);
    }

    private static class FileToMultipartFile implements MultipartFile {
        private final File file;
        public FileToMultipartFile(File file) { this.file = file; }
        @Override public String getName() { return "file"; }
        @Override public String getOriginalFilename() { return file.getName(); }
        @Override public String getContentType() { return "image/png"; }
        @Override public boolean isEmpty() { return file.length() == 0; }
        @Override public long getSize() { return file.length(); }
        @Override public byte[] getBytes() throws IOException { return Files.readAllBytes(file.toPath()); }
        @Override public InputStream getInputStream() throws IOException { return new FileInputStream(file); }
        @Override public void transferTo(File dest) throws IOException { Files.copy(file.toPath(), dest.toPath()); }
    }
}
