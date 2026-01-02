package com.zriyo.aicodemother.ai;

import com.zriyo.aicodemother.model.dto.ModificationPlanDTO;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface AiCodeOptimizerService {


    /**
     * 添加功能/修改功能
     */
    @SystemMessage(fromResource = "prompt/vue3_add_feature.txt")
    ModificationPlanDTO addFeature(@UserMessage String userMessage);
}
