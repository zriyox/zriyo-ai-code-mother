package com.zriyo.aicodemother.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class ModificationPlanDTO {

    // AI 的思考过程
    private String thought;


    private List<FileTask> tasks;

    @Data
    public static class FileTask {
        private String filePath;
        private ActionType action;
        private List<String> referenceFiles;
        private String interfaceDef;
        private String instruction;
        private String description;
        private String fileType;
        private String fileDescription;
        private List<String> exports;
    }

    public enum ActionType {
        CREATE, MODIFY, DELETE
    }
}
