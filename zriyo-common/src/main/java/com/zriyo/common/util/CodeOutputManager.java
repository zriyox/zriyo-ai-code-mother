package com.zriyo.common.util;

import com.zriyo.common.constant.AppConstant;
import com.zriyo.common.exception.BusinessException;
import com.zriyo.common.exception.ErrorCode;
import jodd.util.StringUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.UUID;
import java.util.stream.Stream;

@Slf4j
public class CodeOutputManager {

    private static final String DEPLOY_SUBDIR = AppConstant.APP_DEPLOY_PATH;
    private static final String ARCHIVE_SUBDIR = AppConstant.APP_HISTORY_PATH;

    /**
     * 获取项目根目录下的 tmp/code_output 基础路径
     */
    public static Path getCodeOutputBaseDir() {
        return Paths.get(System.getProperty("user.dir"))
                .resolve(Paths.get(AppConstant.TMP_DIR, AppConstant.APP_GEN_FILE_PATH));
    }

    /**
     * 获取源目录：tmp/code_output/{dirName}
     */
    public static Path getSourceDirectory(String dirName) {
        return getCodeOutputBaseDir().resolve(dirName + "/" + AppConstant.BUILD_OUTPUT_DIR);
    }

    /**
     * 获取部署父目录：tmp/code_output/code_deploy 或自定义子路径
     */
    public static Path getDeployDirectory(String path) {
        if (StringUtil.isNotBlank(path)) {
            return getCodeOutputBaseDir().resolve(path);
        } else {
            return getCodeOutputBaseDir().resolve(DEPLOY_SUBDIR);
        }
    }

    /**
     * 将整个目录复制到 code_deploy 下，并重命名为 targetName
     * 注意：清空目标目录下的文件，但保留子目录（如版本目录）
     */
    public static Path copyHtmlDirToDeploy(String sourceName, String targetName, String path) {
        Path source = getSourceDirectory(sourceName);
        Path deployParent = getDeployDirectory(path);
        Path target = deployParent.resolve(targetName);

        if (!Files.exists(source)) {
            log.error("❌ 源目录不存在: {}", sourceName);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "源目录不存在: " + source);
        }
        if (!Files.isDirectory(source)) {
            log.error("❌ 源路径不是目录: {}", sourceName);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "源路径不是目录: " + source);
        }

        try {
            Files.createDirectories(deployParent);

            if (Files.exists(target)) {
                if (!Files.isDirectory(target)) {
                    Files.delete(target);
                    Files.createDirectories(target);
                    log.warn("⚠️ 原目标为文件，已替换为目录: {}", target);
                } else {
                    log.info("🔄 清空部署目录内容（保留子目录）: {}", target);
                    clearDirectoryFilesOnly(target);
                }
            } else {
                Files.createDirectories(target);
            }

            copyDirectory(source, target);
            log.info("✅ 部署成功: {} → {}", source, target);
            return target;

        } catch (IOException e) {
            log.error("❌ 部署失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "部署失败: " + e.getMessage());
        }
    }

    /**
     * 清空目录下的所有文件，但保留子目录
     */
    public static void clearDirectoryFilesOnly(Path dir) throws IOException {
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            return;
        }
        try (Stream<Path> stream = Files.list(dir)) {
            stream.filter(Files::isRegularFile)
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException e) {
                            throw new RuntimeException("删除文件失败: " + p, e);
                        }
                    });
        }
    }

    /**
     * 清空目录下的所有内容（文件和子目录），但保留该目录本身
     */
    public static void clearDirectory(Path dir) throws IOException {
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            return;
        }
        try (Stream<Path> stream = Files.list(dir)) {
            stream.sorted((a, b) -> -a.compareTo(b))
                    .forEach(p -> {
                        try {
                            deleteRecursively(p);
                        } catch (IOException e) {
                            throw new RuntimeException("清理子项失败: " + p, e);
                        }
                    });
        }
    }

    /**
     * 递归复制整个目录树
     */
    public static void copyDirectory(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path targetDir = target.resolve(source.relativize(dir));
                Files.createDirectories(targetDir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path targetFile = target.resolve(source.relativize(file));
                Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * 递归删除路径（文件或目录）
     */
    public static void deleteRecursively(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (Stream<Path> stream = Files.walk(path)) {
                stream.sorted((a, b) -> -a.compareTo(b))
                        .forEach(p -> {
                            try {
                                Files.delete(p);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
            }
        } else {
            Files.delete(path);
        }
    }

    // ===== 查询与调试方法 =====

    public static boolean hasSubDirectory(String dirName) {
        Path candidate = getCodeOutputBaseDir().resolve(dirName);
        return Files.exists(candidate) && Files.isDirectory(candidate);
    }

    public static void listAllSubDirs() {
        Path base = getCodeOutputBaseDir();
        if (!Files.exists(base)) {
            log.warn("📁 code_output 目录不存在");
            return;
        }
        try (Stream<Path> paths = Files.list(base)) {
            log.info("📁 当前 code_output 下的子目录:");
            paths.filter(Files::isDirectory)
                    .map(p -> p.getFileName().toString())
                    .forEach(d -> log.info("  - {}", d));
        } catch (IOException e) {
            log.error("列出目录失败", e);
        }
    }

    /**
     * 将当前部署实例目录的内容归档到独立的 code_version 目录下：
     * 源: tmp/code_output/code_deploy/{deployInstanceName}
     * 目标: tmp/code_output/code_version/{deployInstanceName}/{version}
     * <p>
     * 注意：跳过源目录中已有的版本子目录（如 v1.0.0），防止嵌套归档。
     */
    public static Path archiveAppVersion(String deployInstanceName, String version) {
        validateDirName(deployInstanceName, "部署实例名");
        validateDirName(version, "版本号");

        Path deployBase = getCodeOutputBaseDir().resolve(DEPLOY_SUBDIR);
        Path source = deployBase.resolve(deployInstanceName);

        Path archiveBase = getCodeOutputBaseDir().resolve(ARCHIVE_SUBDIR);
        Path target = archiveBase.resolve(deployInstanceName).resolve(version);

        if (!Files.exists(source)) {
            log.error("❌ 部署实例目录不存在，无法归档: {}", source.toAbsolutePath());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "部署实例目录不存在: " + source);
        }
        if (!Files.isDirectory(source)) {
            log.error("❌ 部署实例路径不是目录: {}", source);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "部署实例路径无效: " + source);
        }

        try {
            Files.createDirectories(target.getParent());

            if (Files.exists(target)) {
                clearDirectory(target);
            } else {
                Files.createDirectories(target);
            }

            try (Stream<Path> stream = Files.list(source)) {
                stream.forEach(p -> {
                    String fileName = p.getFileName().toString();
                    if (fileName.startsWith("v") && fileName.matches("v\\d+\\.\\d+\\.\\d+(?:[-_.].*)?")) {
                        log.debug("⏭️ 跳过版本目录，不归档: {}", fileName);
                        return;
                    }

                    try {
                        Path targetPath = target.resolve(p.getFileName());
                        if (Files.isDirectory(p)) {
                            copyDirectory(p, targetPath);
                        } else {
                            Files.copy(p, targetPath, StandardCopyOption.REPLACE_EXISTING);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException("复制失败: " + p, e);
                    }
                });
            }

            log.info("✅ 部署实例归档成功: {} → {}", source, target);
            return target;

        } catch (IOException e) {
            log.error("❌ 归档部署实例失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "归档失败: " + e.getMessage());
        }
    }

    /**
     * 删除指定应用的某个历史版本（从 code_version 中删除）
     */
    public static void deleteAppHistoryVersion(String appName, String version) {
        validateDirName(appName, "应用名");
        validateDirName(version, "版本号");

        Path archiveBase = getCodeOutputBaseDir().resolve(ARCHIVE_SUBDIR);
        Path target = archiveBase.resolve(appName).resolve(version);

        if (!Files.exists(target)) {
            log.warn("⚠️ 历史版本目录不存在，无需删除: {}", target);
            return;
        }

        try {
            deleteRecursively(target);
            Path appDir = target.getParent();
            try (Stream<Path> files = Files.list(appDir)) {
                if (!files.findAny().isPresent()) {
                    Files.delete(appDir);
                    log.info("🧹 应用目录已空，一并删除: {}", appDir);
                }
            }
            log.info("✅ 应用历史版本删除成功: {}", target);
        } catch (IOException e) {
            log.error("❌ 删除应用历史版本失败: {}", target, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除失败: " + e.getMessage());
        }
    }

    /**
     * 从历史归档（code_version）部署指定版本到 code_deploy/{appName} 目录
     * （用于恢复、克隆或初始化一个应用实例）
     */
    public static void deployFromHistory(String appName, String version) {
        validateDirName(appName, "应用名");
        validateDirName(version, "版本号");

        Path historyBase = getCodeOutputBaseDir().resolve(ARCHIVE_SUBDIR);
        Path source = historyBase.resolve(appName).resolve(version);
        Path deployParent = getDeployDirectory(null);
        Path target = deployParent.resolve(appName); // ← 直接用 appName 作为目标目录

        if (!Files.exists(source)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "历史版本不存在: " + source);
        }

        try {
            Files.createDirectories(deployParent);
            if (Files.exists(target)) {
                clearDirectory(target);
            } else {
                Files.createDirectories(target);
            }
            copyDirectory(source, target);
            log.info("✅ 从历史版本部署成功: {} → {}", source, target);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "部署失败: " + e.getMessage());
        }
    }

    /**
     * 将指定部署实例的某个历史版本（来自 code_version），部署（覆盖）到其当前运行目录（code_deploy/{instanceName}）
     */
    public static void deployVersionToInstance(String instanceName, String version) {
        validateDirName(instanceName, "部署实例名");
        validateDirName(version, "版本号");

        Path archiveBase = getCodeOutputBaseDir().resolve(ARCHIVE_SUBDIR);
        Path source = archiveBase.resolve(instanceName).resolve(version);
        Path target = getCodeOutputBaseDir().resolve(DEPLOY_SUBDIR).resolve(instanceName);

        if (!Files.exists(source)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "历史版本不存在: " + source);
        }
        if (!Files.isDirectory(source)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "历史版本路径无效: " + source);
        }

        try {
            if (!Files.exists(target)) {
                Files.createDirectories(target);
            } else if (!Files.isDirectory(target)) {
                Files.delete(target);
                Files.createDirectories(target);
            }

            log.info("🔄 正在将版本 {} 部署到实例 {}: 清空当前内容...", version, instanceName);
            clearDirectory(target);

            copyDirectory(source, target);
            log.info("✅ 版本 {} 已成功部署到实例 {}", version, instanceName);

        } catch (IOException e) {
            log.error("❌ 部署版本 {} 到实例 {} 失败: {}", version, instanceName, e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "版本部署失败: " + e.getMessage());
        }
    }

    // ===== 工具方法：校验目录名安全性 =====
    private static void validateDirName(String name, String fieldName) {
        if (StringUtil.isBlank(name)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, fieldName + "不能为空");
        }
        if (name.contains("..") || name.contains("/") || name.contains("\\") || name.startsWith("-")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, fieldName + "不能包含特殊字符或路径分隔符");
        }
    }

    // ===== 测试示例 =====
    public static void main(String[] args) {
        String sourceDir = "1";
        String targetDir = "测试移动_" + UUID.randomUUID().toString();

        if (hasSubDirectory(sourceDir)) {
            log.info("✅ 源目录存在，开始复制...");
            Path result = copyHtmlDirToDeploy(sourceDir, targetDir, null);
            if (result != null) {
                log.info("🎯 部署完成");

                // 示例：归档
                archiveAppVersion(targetDir, "v1.0.0");

                // 示例：回滚到 v1.0.0
                deployVersionToInstance(targetDir, "v1.0.0");

                listAllSubDirs();
            }
        } else {
            log.error("❌ 源目录不存在: {}", sourceDir);
            listAllSubDirs();
        }
    }
}
