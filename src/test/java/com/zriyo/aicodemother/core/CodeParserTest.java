package com.zriyo.aicodemother.core;

import com.zriyo.aicodemother.ai.AiCodeGenTypeRoutingService;
import com.zriyo.aicodemother.ai.factory.AiCodeGeneratorServiceFactory;
import com.zriyo.aicodemother.core.pipeline.CodeGenPipelineBuilder;
import com.zriyo.aicodemother.core.pipeline.GenerationContext;
import com.zriyo.aicodemother.core.pipeline.handler.CodeIntegrityCheckHandler;
import com.zriyo.aicodemother.model.dto.ProjectSkeletonDTO;
import com.zriyo.aicodemother.model.enums.CodeGenTypeEnum;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;

import java.util.HashMap;

@SpringBootTest()
@ActiveProfiles("dev") // ← 强制使用 dev 配置

class CodeParserTest {

    @Autowired
    private AiCodeGenTypeRoutingService aiCodeGenTypeRoutingService;



   @Autowired
   AiCodeGeneratorServiceFactory aiCodeGeneratorFactory;
    @Autowired
    private CodeGenPipelineBuilder pipelineBuilder;


    @Autowired
    private CodeIntegrityCheckHandler codeIntegrityCheckHandler;
    @Test
    void parseHtmlCode() {

        GenerationContext context = new GenerationContext();
        context.setAppId(362243278629937152L);
        context.setMessageId(1L);
        context.setMessage("创建一个博客应用");
        context.setUserId(354802219931017216L);
        context.setToolMassageId(1L);
        context.setIsFirstBuild(true);
        context.setIsOosUrl(false);
        // 初始化上下文和 DTO
        ProjectSkeletonDTO projectSkeletonDTO = new ProjectSkeletonDTO();
        ProjectSkeletonDTO.GlobalInfo globalInfo = new ProjectSkeletonDTO.GlobalInfo();

        globalInfo.setDescription("A blog application");
        globalInfo.setDependencies(new HashMap<>());

        // styleGuide 设置
        HashMap<String, Object> stylelint = new HashMap<>();
        stylelint.put("stylelint", "11");
        globalInfo.setStyleGuide(stylelint);

        projectSkeletonDTO.setGlobal(globalInfo);
        context.setSkeleton(projectSkeletonDTO);
        context.setCodeGenType(CodeGenTypeEnum.VUE_PROJECT);

        Flux<ServerSentEvent<Object>> integrityCheckStream = codeIntegrityCheckHandler.handle(context);

        Flux<ServerSentEvent<Object>> withCompletionSignal = integrityCheckStream.concatWith(
                Flux.just(
                        ServerSentEvent.<Object>builder()
                                .event("lint_complete")          // 自定义事件名
                                .data("✅ 项目语法检查已完成")     // 消息内容
                                .build()
                )
        );
        withCompletionSignal
                .doOnNext(event -> System.out.println("SSE: " + event))
                .blockLast(); // 阻塞直到流结束（适合测试）

    }

    @Test
    void parseMultiFileCode() {
//
//        MultiFileCodeResult result = CodeParser.parseMultiFileCode(codeContent);
//        assertNotNull(result);
//        assertNotNull(result.getHtmlCode());
//        assertNotNull(result.getCssCode());
//        assertNotNull(result.getJsCode());
    }
}
