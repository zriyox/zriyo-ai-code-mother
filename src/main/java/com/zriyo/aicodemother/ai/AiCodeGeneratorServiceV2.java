package com.zriyo.aicodemother.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface AiCodeGeneratorServiceV2 {

    @SystemMessage(fromResource = "prompt/codegen-vue-one-context-prompt.txt")
    @UserMessage("Please generate the Vue project code based on the context provided in the system message.")
    TokenStream generateVueProjectCodeTokenStreamTest(@V("userMessage") String userMessage);

    /**
     * 生成基础设施文件 (main.js, App.vue, router)
     */
    @SystemMessage(fromResource = "prompt/codegen-infra-prompt.txt")
    @UserMessage("Build core infra: {{userMessage}}")
    TokenStream generateInfraCodeStream(@V("userMessage") String userMessage);

    /**
     * 生成通用组件或页面文件
     */
    @SystemMessage(fromResource = "prompt/codegen-component-prompt.txt")
    @UserMessage("Build component: {{userMessage}}")
    TokenStream generateComponentCodeStream(@V("userMessage") String userMessage);


    /**
     * 检查 vue 项目 bug 最后阶段
     */
    @SystemMessage(fromResource = "prompt/vue3_fix_bug.txt")
    @UserMessage("Please analyze the error logs and source code, then apply the fix using the provided tools.")
    TokenStream checkVueProjectBugTokenStream(@V("userMessage") String userMessage);



    /**
     * 修复运行时逻辑错误
     */
    @SystemMessage(fromResource = "prompt/vue3_runtime_logic_fix.txt")
    @UserMessage("Please diagnose and fix the runtime logic error according to the feedback and source code provided.")
    TokenStream fixRuntimeLogicBugTokenStream(@V("userMessage") String userMessage);


    /**
     * 优化提示词
     */
    @SystemMessage(fromResource = "prompt/vue3_prompt_optimizer.txt")
    TokenStream optimizePromptTokenStream( String userMessage);


}
