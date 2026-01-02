package com.zriyo.aicodemother.ai;

import com.zriyo.aicodemother.model.dto.FaultyFileReportDTO;
import com.zriyo.aicodemother.model.dto.InvestigationResult;
import com.zriyo.aicodemother.model.dto.ModificationPlanDTO;
import com.zriyo.aicodemother.model.dto.ProjectSkeletonDTO;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * AI代码生成类型智能路由服务
 * 使用结构化输出直接返回枚举类型
 *
 * @author yupi
 */
public interface AiCodeGenTypeRoutingService {

    /**
     * 根据用户需求智能选择代码生成类型
     *
     * @param userPrompt 用户输入的需求描述
     * @return 推荐的代码生成类型
     */
    @SystemMessage(fromResource = "prompt/codegen-routing-system-prompt.txt")
    AppNameResult routeCodeGenType(String userPrompt);

    /**
     * 总结上下文
     */
    @SystemMessage(fromResource = "prompt/codegen-summarize-context-prompt.txt")
    String summarizeContext(@UserMessage String userMessage);

    /**
     * 项目初始化 生成提示词
     */
    @SystemMessage(fromResource = "prompt/vue3_zero_code_project_init.txt")
    ProjectSkeletonDTO initVueProject(@UserMessage String userMessage);

    /**
     * 修复 bug
     */
    @SystemMessage(fromResource = "prompt/vue3_fix_bug.txt")
    String fixBug(@UserMessage String userMessage);


    public record AppNameResult(String appName) {}


    /**
     * 智能故障分诊：当规则无法定位时，由 AI 根据骨架和报错信息判断目标文件
     */
    @SystemMessage(fromResource = "prompt/vue3_error_dispatcher.txt")
    @UserMessage("Identify the faulty file path from the project skeleton based on the provided error context.")
    FaultyFileReportDTO dispatchFaultyFile(@V("userMessage") String userMessage);

    /**
     * 提示词智能优化：剔除技术栈描述，提升指令感
     */
    @SystemMessage(fromResource = "prompt/prompt-optimization.txt")
    String optimizeUserPrompt(String userMessage);

    /**
     * 添加功能/修改功能
     */
    @SystemMessage(fromResource = "prompt/vue3_add_feature.txt")
    ModificationPlanDTO addFeature(@UserMessage String userMessage);

    /**
     * 文件探测
     */
    @SystemMessage(fromResource = "prompt/investigation-prompt.txt")
    InvestigationResult probeFile(@UserMessage String userMessage);
}
