package com.zriyo.aicodemother.ai.service;

import com.zriyo.aicodemother.ai.AiCodeGenTypeRoutingService;
import com.zriyo.aicodemother.model.dto.FaultyFileReportDTO;
import com.zriyo.aicodemother.model.dto.InvestigationResult;
import com.zriyo.aicodemother.model.dto.ModificationPlanDTO;
import com.zriyo.aicodemother.model.dto.ProjectSkeletonDTO;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class AiCodeGenTypeRoutingServiceImpl {

    private final AiCodeGenTypeRoutingService aiService;

    @Retryable(
            retryFor = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2),
            recover = "recoverInitVueProject"
    )
    public ProjectSkeletonDTO initVueProject(String userMessage) {
        ProjectSkeletonDTO projectSkeletonDTO = aiService.initVueProject(userMessage);
        return projectSkeletonDTO;
    }
    @Recover
    public ProjectSkeletonDTO recoverInitVueProject(Exception ex, String userMessage) {
        log.error("AI 创建骨架失败，系统内部错误: {}", ex.getMessage());
        throw new RuntimeException("AI 生成骨架失败，系统内部错误", ex);
    }

    @Retryable(
            retryFor = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2),
            recover = "dispatchFaultyFile"
    )
    public FaultyFileReportDTO dispatchFaultyFile(String message){
        FaultyFileReportDTO report = aiService.dispatchFaultyFile(message);
        return report;
    }
    @Recover
    public FaultyFileReportDTO dispatchFaultyFile(Exception ex, String message){
        log.error("AI 错误定位失败，系统内部错误: {}", ex.getMessage());
        throw new RuntimeException("AI 错误定位失败，系统内部错误", ex);
    }

    @Retryable(
            retryFor = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2),
            recover = "optimizeUserPrompt"
    )
    public String optimizeUserPrompt(String message){
        String optimized = aiService.optimizeUserPrompt(message);
        return optimized;
    }
    @Recover
    public String optimizeUserPrompt(Exception ex, String message){
        log.error("AI 优化用户输入失败，系统内部错误: {}", ex.getMessage());
        throw new RuntimeException("AI 优化用户输入失败，系统内部错误", ex);
    }

    @Retryable(
            retryFor = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2),
            recover = "addFeature"
    )
    public ModificationPlanDTO addFeature(String prompt) {
        return aiService.addFeature(prompt);
    }
    @Recover
    public String addFeature(Exception ex, String message){
        log.error("AI 修改文件失败，系统内部错误: {}", ex.getMessage());
        throw new RuntimeException("AI 优化用户输入失败，系统内部错误", ex);
    }
    @Retryable(
            retryFor = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2),
            recover = "investigation"
    )
    public InvestigationResult investigation(String prompt) {
        return aiService.probeFile(prompt);
    }
    @Recover
    public InvestigationResult investigation(Exception ex, String message){
        log.error("AI 定位文件失败，系统内部错误: {}", ex.getMessage());
        throw new RuntimeException("AI 定位文件失败，系统内部错误", ex);
    }
}
