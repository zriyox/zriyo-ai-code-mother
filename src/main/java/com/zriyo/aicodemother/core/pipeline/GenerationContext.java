package com.zriyo.aicodemother.core.pipeline;

import com.zriyo.aicodemother.model.dto.ModificationPlanDTO;
import com.zriyo.aicodemother.model.dto.ProjectSkeletonDTO;
import com.zriyo.aicodemother.model.dto.RuntimeFeedbackDTO;
import com.zriyo.aicodemother.model.entity.AiCodeGenRecord;
import com.zriyo.aicodemother.model.enums.CodeGenTypeEnum;
import lombok.Data;

import java.util.List;

@Data
public class GenerationContext {

    // ===== 基础输入 =====
    private Long appId;
    private Long userId;
    private String message;
    private Long messageId;
    //是否是第一次构建
    private Boolean isFirstBuild;
    //oosUrl 是否为空
    private Boolean isOosUrl;
    // ===== 流程中间产物 =====
    private ProjectSkeletonDTO skeleton; // 项目骨架（由 SkeletonGenerateHandler 生成）
    private AiCodeGenRecord record;
    private String projectDir;
    private List<String> generatedFiles;
    private Long skeletonId;
    private Long toolMassageId;
    private String oosUrl;
    //修改产物
    private ModificationPlanDTO modificationPlan;
    // 运行时反馈（前端上报）
    private RuntimeFeedbackDTO runtimeFeedback;
    // ===== 控制标志 =====
    private boolean terminated = false;
    //是否报错
    private Boolean IsError = false;
    private CodeGenTypeEnum codeGenType;



}
