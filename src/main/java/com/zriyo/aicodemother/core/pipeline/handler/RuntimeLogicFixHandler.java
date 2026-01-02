package com.zriyo.aicodemother.core.pipeline.handler;

import cn.hutool.json.JSONUtil;
import com.zriyo.aicodemother.ai.AiCodeGeneratorServiceV2;
import com.zriyo.aicodemother.ai.factory.AiCodeGeneratorServiceFactoryV2;
import com.zriyo.aicodemother.ai.service.AiCodeGenTypeRoutingServiceImpl;
import com.zriyo.aicodemother.core.handler.AiContextHolder;
import com.zriyo.aicodemother.core.pipeline.GenerationContext;
import com.zriyo.aicodemother.core.pipeline.service.CodeGenRecordService;
import com.zriyo.aicodemother.model.AppConstant;
import com.zriyo.aicodemother.model.MonitorContext;
import com.zriyo.aicodemother.model.dto.FaultyFileReportDTO;
import com.zriyo.aicodemother.model.dto.RuntimeFeedbackDTO;
import com.zriyo.aicodemother.model.enums.AiCodeGenStage;
import com.zriyo.aicodemother.model.enums.ToolAction;
import com.zriyo.aicodemother.model.message.StreamMessageTypeEnum;
import com.zriyo.aicodemother.service.AiToolLogService;
import com.zriyo.aicodemother.service.ChatHistoryService;
import com.zriyo.aicodemother.util.SseEventBuilder;
import dev.langchain4j.service.TokenStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.annotation.Order;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Order(5)
@Slf4j
public class RuntimeLogicFixHandler extends AbstractCodeGenHandler {

    private final AiCodeGeneratorServiceFactoryV2 aiCodeGeneratorServiceFactory;
    private static final int GLOBAL_CHAR_LIMIT = 28000;

    public RuntimeLogicFixHandler(AiCodeGeneratorServiceFactoryV2 aiCodeGeneratorServiceFactory,
                                  CodeGenRecordService codeGenRecordService,
                                  ChatHistoryService chatHistoryService,
                                  AiToolLogService aiToolLogService,
                                  ApplicationEventPublisher publisher,
                                  AiCodeGenTypeRoutingServiceImpl aiCodeGenTypeRoutingService) {
        super(codeGenRecordService, chatHistoryService, aiToolLogService, publisher, aiCodeGenTypeRoutingService);
        this.aiCodeGeneratorServiceFactory = aiCodeGeneratorServiceFactory;
    }

    @Override
    protected AiCodeGenStage getStage() {
        return AiCodeGenStage.RUNTIME_DIAGNOSIS;
    }

    @Override
    protected boolean shouldSkip(GenerationContext context) {
        return context.getRuntimeFeedback() == null;
    }

    @Override
    protected Flux<ServerSentEvent<Object>> doExecute(GenerationContext context) {
        Long appId = context.getAppId();
        String projectDirName = AppConstant.VUE_PROJECT_PREFIX + appId;
        String projectRoot = AppConstant.TMP_DIR + "/" + AppConstant.APP_GEN_FILE_PATH + "/" + projectDirName;

        return Flux.<ServerSentEvent<Object>>create(emitter -> {
            AiContextHolder.set(MonitorContext.builder().appId(String.valueOf(appId)).userId(String.valueOf(context.getUserId())).build());
            try {
                emitter.next(SseEventBuilder.of(StreamMessageTypeEnum.DIAGNOSIS_PROCESS, "AI 专家正在分析故障全景图..."));

                FaultyFileReportDTO report = resolveFaultyFile(context, context.getRuntimeFeedback());

                if (report == null || report.getFaultyFiles() == null || report.getFaultyFiles().isEmpty()) {
                    emitter.next(SseEventBuilder.of(StreamMessageTypeEnum.DIAGNOSIS_ERROR, "无法定位故障源，请补充反馈信息。"));
                    return;
                }

                emitter.next(SseEventBuilder.of(StreamMessageTypeEnum.DIAGNOSIS_PROCESS, "已锁定故障链，正在同步下发修复指令..."));

                String prompt = buildMultiFileFixPrompt(context, projectRoot, report.getFaultyFiles());

                String primaryFile = report.getFaultyFiles().get(0).getPath();
                aiCodeGeneratorServiceFactory.invalidateService(primaryFile, context.getCodeGenType());
                AiCodeGeneratorServiceV2 aiService = aiCodeGeneratorServiceFactory.getAiErrorCodeGeneratorService(
                        primaryFile, context.getCodeGenType(), projectDirName, context.getAppId());

                handleAiFixStream(aiService, prompt, context).toIterable().forEach(emitter::next);

                emitter.next(SseEventBuilder.of(StreamMessageTypeEnum.DIAGNOSIS_PROCESS, "故障已跨文件同步修复完成！准备进入构建阶段.."));
            } catch (Exception e) {
                log.error("Logic fix failed", e);
                emitter.next(SseEventBuilder.of(StreamMessageTypeEnum.AI_RESPONSE, "修复中断: " + e.getMessage()));
            } finally {
                emitter.complete();
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private String buildMultiFileFixPrompt(GenerationContext context, String projectRoot, List<FaultyFileReportDTO.FaultyFileReport> faults) {
        StringBuilder sb = new StringBuilder();
        String SURGEON_PROTOCOL =
                "# Role: Vue 3 Runtime Surgeon (Strict Mode)\n" +
                        "## 🛠 修复准则\n" +
                        "1. **最小干预**: 仅修复 Bug 关键点，严禁重构无关代码。\n" +
                        "2. **全景修复**: 你已获得所有相关文件的读写权限。若修复涉及多文件联动（如修改 template 增加 id，并在 script 中引用），请在同一回合内完成所有文件的 writeFile 调用。\n" +
                        "3. **非法选择器禁令**: 严禁在 JS 中使用 querySelector 查找带方括号的 Tailwind 类名（如 .bg-[#...]）。必须通过添加 id 或 ref 属性来定位元素。\n" +
                        "4. **符号恢复**: 必须严格恢复被转义的符号。将美元符紧跟左大括号恢复为变量语法，将连续的两个左大括号恢复为插槽语法，将连续的两个右大括号恢复为闭合语法。严禁在最终输出中保留多余的空格。\n" +
                        "5. **续写协议**: 若代码超长，请分段执行 writeFile 和 continueWriting。续写开头可重复前文末尾 20 字符以确保衔接，系统会自动去重。";
        sb.append(SURGEON_PROTOCOL).append("\n");
        sb.append("# 🏥 故障诊断报告\n")
                .append("- 需求背景: ").append(context.getMessage()).append("\n")
                .append("- 运行时报错: ").append(context.getRuntimeFeedback().getErrorMsg()).append("\n")
                .append("- 错误上下文: ").append(context.getRuntimeFeedback().getContext()).append("\n\n");

        sb.append("# 📄 待修复源码清单 (已注入上下文)\n");
        for (FaultyFileReportDTO.FaultyFileReport f : faults) {
            String content = readFileContentSafe(projectRoot, f.getPath());
            sb.append("\n--- 文件路径: ").append(f.getPath()).append(" ---\n")
                    .append("诊断分析: ").append(f.getAnalysis()).append("\n")
                    .append("```vue\n").append(content).append("\n```\n");
        }

        sb.append("\n## ⛔ 终极执行指令\n")
                .append("1. 分析以上所有关联文件，判断是否存在逻辑或调用耦合。\n")
                .append("2. 请连续调用 `writeFile` 为每个需要修改的文件下发补丁。\n")
                .append("3. 全部修改完成后，必须调用 `finishRepair` 退出会话。");
        return sb.toString();
    }

    private String readFileContentSafe(String projectRoot, String relativePath) {
        try {
            String cleanPath = relativePath.trim().replace("@/", "src/");
            if (cleanPath.startsWith("/") || cleanPath.startsWith("\\")) cleanPath = cleanPath.substring(1);

            Path path = Paths.get(projectRoot, cleanPath);
            if (!Files.exists(path)) return "// [File Not Found]";

            return Files.readString(path)
                    .replace("${", "$ {")
                    .replace("{{", "{ {")
                    .replaceAll("(?m)^\\s*\\r?\\n", "")
                    .trim();
        } catch (Exception e) {
            return "// [Read Error]";
        }
    }

    private Flux<ServerSentEvent<Object>> handleAiFixStream(AiCodeGeneratorServiceV2 aiService, String prompt, GenerationContext context) {
        return Flux.create(sink -> {
            AiContextHolder.set(MonitorContext.builder().appId(String.valueOf(context.getAppId())).userId(String.valueOf(context.getUserId())).build());
            long startTime = System.currentTimeMillis();
            TokenStream tokenStream = super.invokeTokenStream(aiService, context, prompt, RUNTIME_FIX);
            AtomicBoolean toolExecuted = new AtomicBoolean(false);

            tokenStream
                    .onPartialResponse(content -> log.debug("AI 分析日志: {}", content))
                    .onPartialToolExecutionRequest((i, r) -> toolExecuted.set(true))
                    .onToolExecuted(exec -> {
                        savaToolLog(context, "MultiFileFix", exec, startTime, ToolAction.FIX_BUG);
                        sink.next(SseEventBuilder.of(StreamMessageTypeEnum.CODE_TOOL_EXECUTED, "✅ 已应用补丁到目标文件"));
                    })
                    .onCompleteResponse(resp -> {
                        if (!toolExecuted.get()) {
                            sink.error(new IllegalStateException("AI 未下发任何 writeFile 指令，请检查报错是否已在之前修复。"));
                            return;
                        }
                        sink.next(SseEventBuilder.of(StreamMessageTypeEnum.TOOL_DONE));
                        sink.complete();
                    })
                    .onError(e -> {
                        log.error("Fix Stream Error: ", e);
                        sink.error(e);
                    })
                    .start();
        });
    }

    private FaultyFileReportDTO resolveFaultyFile(GenerationContext context, RuntimeFeedbackDTO feedback) {
        return aiCodeGenTypeRoutingService.dispatchFaultyFile(
                String.format("【Bug】: %s\n【Trace】: %s\n【Skeleton】: %s",
                        feedback.getErrorMsg(), feedback.getContext(), JSONUtil.toJsonStr(context.getSkeleton()))
        );
    }
}
