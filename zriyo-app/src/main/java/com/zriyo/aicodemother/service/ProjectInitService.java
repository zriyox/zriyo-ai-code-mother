package com.zriyo.aicodemother.service;

import com.zriyo.common.exception.BusinessException;
import com.zriyo.common.result.ErrorCode;
import com.zriyo.common.util.FrontendScaffoldUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 项目初始化服务（Java 侧确定性动作）
 *
 * <p>说明：</p>
 * <ol>
 *   <li>不调用 LLM</li>
 *   <li>仅做模板复制与 node_modules 软链接</li>
 *   <li>用于将初始化动作收敛到 Java 编排层</li>
 * </ol>
 *
 * @author Zriyo AI
 * @since 2026-03-01
 */
@Slf4j
@Service
public class ProjectInitService {

    /**
     * 项目根目录（可相对项目启动目录）
     */
    @Value("${project.base-path:tmp}")
    private String projectBasePath;

    /**
     * 前端脚手架目录（可相对项目启动目录）
     */
    @Value("${frontend.scaffold-path:frontend-scaffold}")
    private String scaffoldPath;

    /**
     * 目录命名模板
     */
    @Value("${project.dir-pattern:app_%06d}")
    private String projectDirPattern;

    /**
     * 初始化项目目录。
     *
     * @param appId       应用 ID
     * @param projectName 项目名（写入 package.json.name）
     * @return 初始化后的绝对路径
     */
    public String initProject(long appId, String projectName) {
        Path base = resolvePath(projectBasePath);
        Path scaffold = resolvePath(scaffoldPath);
        Path project = base.resolve(String.format(projectDirPattern, appId)).normalize();

        try {
            FrontendScaffoldUtils.initProject(scaffold, project, projectName);
            return project.toString();
        } catch (IOException e) {
            log.error("初始化项目失败 appId={}, project={}, error={}", appId, project, e.getMessage(), e);
            throw new BusinessException(ErrorCode.OPERATION_FAILED, "初始化项目失败: " + e.getMessage());
        }
    }

    private Path resolvePath(String raw) {
        Path path = Paths.get(raw);
        if (path.isAbsolute()) {
            return path.normalize();
        }
        return Paths.get(System.getProperty("user.dir")).resolve(path).normalize();
    }
}

