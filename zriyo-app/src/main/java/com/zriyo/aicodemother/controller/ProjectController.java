package com.zriyo.aicodemother.controller;

import com.zriyo.aicodemother.model.dto.project.GenerateProjectRequest;
import com.zriyo.aicodemother.model.dto.project.GenerateProjectResponse;
import com.zriyo.aicodemother.service.ProjectInitService;
import com.zriyo.common.result.Result;
import com.zriyo.common.result.ResultUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 项目初始化控制器（后端编排入口）。
 *
 * @author Zriyo AI
 * @since 2026-03-04
 */
@RestController
@RequestMapping("/api/v1/project")
@RequiredArgsConstructor
public class ProjectController {

    private static final String DEFAULT_PROJECT_NAME = "ai-generated-app";

    private final ProjectInitService projectInitService;

    @PostMapping("/generate")
    public Result<GenerateProjectResponse> generate(@Valid @RequestBody GenerateProjectRequest request) {
        String projectName = StringUtils.hasText(request.getProjectName())
                ? request.getProjectName()
                : DEFAULT_PROJECT_NAME;

        String projectPath = projectInitService.initProject(request.getAppId(), projectName);

        GenerateProjectResponse response = new GenerateProjectResponse();
        response.setSuccess(true);
        response.setProjectPath(projectPath);
        return ResultUtils.success(response);
    }
}
