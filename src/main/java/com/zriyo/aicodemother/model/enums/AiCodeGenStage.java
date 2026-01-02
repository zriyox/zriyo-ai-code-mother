package com.zriyo.aicodemother.model.enums;

/**
 * AI 代码生成的当前阶段
 */
public enum AiCodeGenStage {
    INIT("INIT", "初始化"),
    SKELETON("SKELETON", "骨架生成"),
    FILE_GENERATION("FILE_GENERATION", "文件生成"),
    //生成代码
    CODE_GENERATION("CODE_GENERATION", "生成代码"),
    //排查 bug 阶段
    DIAGNOSIS("DIAGNOSIS", "排查 bug"),
    //修改
    MODIFY("MODIFY", "修改"),
    //加载骨架
    LOAD_SKELETON("LOAD_SKELETON", "加载骨架"),
    BUILD("BUILD", "构建打包"),
    //运行时诊断
    RUNTIME_DIAGNOSIS("RUNTIME_DIAGNOSIS", "运行时诊断"),
    DONE("DONE", "完成");

    private final String value;
    private final String description;

    AiCodeGenStage(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    public static AiCodeGenStage fromValue(String value) {
        for (AiCodeGenStage stage : values()) {
            if (stage.value.equals(value)) {
                return stage;
            }
        }
        throw new IllegalArgumentException("Unknown stage: " + value);
    }
}
