package com.zriyo.aicodemother.service;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import org.apache.logging.log4j.util.BiConsumer;

public interface TokenStream {
    TokenStream onPartialToolExecutionRequest(BiConsumer<Integer, ToolExecutionRequest> consumer);

    TokenStream onCompleteToolExecutionRequest(BiConsumer<Integer, ToolExecutionRequest> consumer);
}
