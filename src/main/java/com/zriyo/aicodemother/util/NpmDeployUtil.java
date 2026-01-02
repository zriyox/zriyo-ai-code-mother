package com.zriyo.aicodemother.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public class NpmDeployUtil {

    @Data
    @AllArgsConstructor
    public static class NpmCommandResult {
        private String command;
        private boolean success;
        private String output;
    }

    @Data
    @AllArgsConstructor
    public static class ProjectBuildResult {
        private NpmCommandResult installResult;
        private NpmCommandResult lintResult;
        private NpmCommandResult buildResult;
    }

    private static ProcessBuilder buildPlatformProcess(String command) {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return new ProcessBuilder("cmd.exe", "/c", command);
        } else {
            return new ProcessBuilder("bash", "-c", command);
        }
    }

    private static NpmCommandResult execCommand(String workDir, String command) {
        StringBuilder output = new StringBuilder();
        boolean success = false;
        try {
            ProcessBuilder pb = buildPlatformProcess(command);
            pb.directory(new java.io.File(workDir));
            pb.redirectErrorStream(true);

            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    log.debug("[NPM] {}", line);
                }
            }

            int exit = process.waitFor();
            success = (exit == 0);
            if (success) {
                log.info("命令执行成功: {}", command);
            } else {
                log.error("命令执行失败 (exit={}): {}", exit, command);
            }

        } catch (Exception e) {
            String errorMsg = "执行命令异常: " + command + " | " + e.getMessage();
            output.append(errorMsg).append("\n");
            log.error(errorMsg, e);
        }
        return new NpmCommandResult(command, success, output.toString());
    }

    /** 安装依赖 */
    public static NpmCommandResult installDependencies(String absolutePath) {
        return execCommand(absolutePath, "npm install");
    }

    /** 对整个项目执行 ESLint 自动修复 */
    public static NpmCommandResult lintFix(String absolutePath) {
        return execCommand(absolutePath, "npx eslint . --ext .js,.vue --fix");
    }

    /** 构建项目 */
    public static NpmCommandResult buildProject(String absolutePath) {
        return execCommand(absolutePath, "npm run build");
    }

    /**
     * 对单个文件执行 ESLint 自动修复
     *
     * @param projectRootAbsolutePath 项目根目录的绝对路径（必须是包含 package.json 的目录）
     * @param relativeFilePath        目标文件相对于项目根目录的路径，例如 "src/components/AppHeader.vue"
     * @return 命令执行结果
     */
    public static NpmCommandResult lintFixOne(String projectRootAbsolutePath, String relativeFilePath) {
        if (projectRootAbsolutePath == null || relativeFilePath == null) {
            String msg = "参数不能为空：projectRootAbsolutePath 或 relativeFilePath 为 null";
            log.warn(msg);
            return new NpmCommandResult("npx eslint <invalid> --fix", false, msg);
        }

        java.io.File projectRoot = new java.io.File(projectRootAbsolutePath);
        if (!projectRoot.exists() || !projectRoot.isDirectory()) {
            String msg = "项目根目录无效（不存在或不是目录）: " + projectRootAbsolutePath;
            log.warn(msg);
            return new NpmCommandResult("npx eslint ... --fix", false, msg);
        }

        // 标准化路径以避免 ../ 等问题
        Path base = Paths.get(projectRootAbsolutePath).normalize();
        Path targetPath = base.resolve(relativeFilePath).normalize();

        // 安全校验：防止路径穿越
        if (!targetPath.startsWith(base)) {
            String msg = "拒绝路径穿越攻击：尝试访问非法路径 " + relativeFilePath;
            log.warn(msg);
            return new NpmCommandResult("npx eslint ... --fix", false, msg);
        }

        java.io.File targetFile = targetPath.toFile();
        if (!targetFile.exists() || targetFile.isDirectory()) {
            String msg = "目标文件不存在或是一个目录: " + targetFile.getAbsolutePath();
            log.warn(msg);
            return new NpmCommandResult("npx eslint \"" + relativeFilePath + "\" --fix", false, msg);
        }

        String fileName = targetFile.getName().toLowerCase();
        if (!fileName.endsWith(".js") && !fileName.endsWith(".vue")) {
            String msg = "仅支持 .js 和 .vue 文件进行 ESLint 修复: " + relativeFilePath;
            log.warn(msg);
            return new NpmCommandResult("npx eslint \"" + relativeFilePath + "\" --fix", false, msg);
        }

        // 构造命令（使用相对路径，确保 ESLint 能读取配置）
        String command = "npx eslint \"" + relativeFilePath + "\" --fix";
        return execCommand(projectRootAbsolutePath, command);
    }

    /**
     * 执行完整构建流程：install → lint → build
     */
    public static ProjectBuildResult buildVueProject(String relativeWorkDir) {
        if (relativeWorkDir == null || relativeWorkDir.isEmpty()) {
            throw new IllegalArgumentException("workDir must not be null or empty");
        }

        Path baseDir = CodeOutputManager.getCodeOutputBaseDir();
        Path resolved = baseDir.resolve(relativeWorkDir).normalize();

        if (!resolved.startsWith(baseDir)) {
            log.warn("Blocked path traversal attempt: {}", relativeWorkDir);
            throw new SecurityException("Invalid workDir path");
        }

        String absolutePath = resolved.toString();
        log.info("Building Vue project in: {}", absolutePath);

        NpmCommandResult installResult = installDependencies(absolutePath);
        NpmCommandResult lintResult = lintFix(absolutePath);
        NpmCommandResult buildResult = buildProject(absolutePath);

        return new ProjectBuildResult(installResult, lintResult, buildResult);
    }

    // ===== 测试用主方法 =====
    public static void main(String[] args) {
        // 示例：完整构建
        // ProjectBuildResult result = buildVueProject("vue_project_347617905128759296");

        // 示例：单文件修复
        String projectRoot = "/your/base/dir/vue_project_347617905128759296"; // 替换为实际路径
        String fileToFix = "src/components/AppHeader.vue";

        NpmCommandResult result = lintFixOne(projectRoot, fileToFix);

        System.out.println("=== Lint Single File Result ===");
        System.out.println("Success: " + result.isSuccess());
        System.out.println("Command: " + result.getCommand());
        System.out.println("Output:\n" + result.getOutput());
    }
}
