package com.zriyo.aicodemother.model.dto;

import lombok.Data;

import java.util.List;

/**
 * 故障文件诊断报告
 */
@Data
public class FaultyFileReportDTO {

    private List<FaultyFileReport> faultyFiles;

    @Data
    public static class FaultyFileReport{
        private String path;
        private String analysis;
    }
}
