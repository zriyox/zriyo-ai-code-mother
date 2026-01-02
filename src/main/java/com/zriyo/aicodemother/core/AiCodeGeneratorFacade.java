package com.zriyo.aicodemother.core;

import cn.hutool.json.JSONUtil;
import com.zriyo.aicodemother.ai.AiCodeGeneratorService;
import com.zriyo.aicodemother.ai.factory.AiCodeGeneratorServiceFactory;
import com.zriyo.aicodemother.core.parser.CodeParserExecutor;
import com.zriyo.aicodemother.core.saver.CodeFileSaverExecutor;
import com.zriyo.aicodemother.exception.BusinessException;
import com.zriyo.aicodemother.exception.ErrorCode;
import com.zriyo.aicodemother.model.enums.CodeGenTypeEnum;
import com.zriyo.aicodemother.model.message.AiResponseMessage;
import com.zriyo.aicodemother.model.message.ToolExecutedMessage;
import com.zriyo.aicodemother.model.message.ToolRequestMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecution;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.File;

/**
 * AI 代码生成门面类，组合代码生成和保存功能
 */
@Service
@Slf4j
public class AiCodeGeneratorFacade {



    @Resource
    private AiCodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;


    /**
     * 统一入口：根据类型生成并保存代码（流式）
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     * @return 保存的目录
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "生成类型不能为空");
        }
        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId, codeGenTypeEnum);

        return switch (codeGenTypeEnum) {
            case HTML -> {
                Flux<String> result = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
                yield processCodeStream(result, CodeGenTypeEnum.HTML, appId);
            }
            case MULTI_FILE -> {
                Flux<String> result = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
                yield processCodeStream(result, CodeGenTypeEnum.MULTI_FILE, appId);
            }
            case VUE_PROJECT -> {
                TokenStream tokenStream = aiCodeGeneratorService.generateVueProjectCodeTokenStream(userMessage);
                yield processTokenStream(tokenStream);
            }
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }

    /**
     * 将 TokenStream 转换为 Flux<String>，并传递工具调用信息
     *
     * @param tokenStream TokenStream 对象
     * @return Flux<String> 流式响应
     */
    private Flux<String> processTokenStream(TokenStream tokenStream) {
        return Flux.create(sink -> {
            tokenStream.onPartialResponse((String partialResponse) -> {
                        AiResponseMessage aiResponseMessage = new AiResponseMessage(partialResponse);
                        sink.next(JSONUtil.toJsonStr(aiResponseMessage));
                    })
                    .onPartialToolExecutionRequest((index, toolExecutionRequest) -> {
                        ToolRequestMessage toolRequestMessage = new ToolRequestMessage(toolExecutionRequest);
                        sink.next(JSONUtil.toJsonStr(toolRequestMessage));
                    })
                    .onToolExecuted((ToolExecution toolExecution) -> {
                        ToolExecutedMessage toolExecutedMessage = new ToolExecutedMessage(toolExecution);
                        sink.next(JSONUtil.toJsonStr(toolExecutedMessage));
                    })
                    .onCompleteResponse((ChatResponse response) -> {
                        sink.complete();
                    })
                    .onError((Throwable error) -> {
                        error.printStackTrace();
                        sink.error(error);
                    })
                    .start();
        });
    }



    private Flux<String> processCodeStream(Flux<String> result, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        Flux<String> shared = result.share();

        // 只有 NON_TOOL 类型才需要从流中提取代码并保存
        if (codeGenTypeEnum == CodeGenTypeEnum.HTML) {
            shared.collect(StringBuilder::new, StringBuilder::append)
                    .map(StringBuilder::toString)
                    .filter(StringUtils::hasText) // 防 null/empty
                    .flatMap(completeCode -> {
                        try {
                            Object codeParser = CodeParserExecutor.getCodeParser(completeCode, codeGenTypeEnum);
                            File saveDir = CodeFileSaverExecutor.saveCode(codeParser, codeGenTypeEnum, appId);
                            log.info("保存成功，目录为：{}", saveDir.getAbsolutePath());
                            return Mono.empty();
                        } catch (Exception e) {
                            log.error("保存失败: ", e);
                            return Mono.error(new BusinessException(ErrorCode.SYSTEM_ERROR));
                        }
                    })
                    .subscribeOn(Schedulers.boundedElastic())
                    .subscribe();

        } else if (codeGenTypeEnum == CodeGenTypeEnum.VUE_PROJECT || codeGenTypeEnum == CodeGenTypeEnum.MULTI_FILE) {
            // ✅ 工具调用模式：代码已由 FileWriteTool 自动写入磁盘
            // 无需从流中提取任何内容！
            // 可选：记录日志或验证文件是否存在
            log.info("Vue 项目生成中，代码将由工具自动写入 appId={}", appId);
            // 不做任何保存操作
        }

        return shared;
    }



}


















