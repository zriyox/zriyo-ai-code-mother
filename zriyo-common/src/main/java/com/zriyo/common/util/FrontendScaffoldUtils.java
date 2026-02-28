package com.zriyo.common.util;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.SimpleFileVisitor;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

/**
 * 前端脚手架初始化工具（确定性，无 LLM）。
 *
 * <p>职责：</p>
 * <ol>
 *   <li>按白名单复制最小模板文件</li>
 *   <li>更新 package.json 的项目名</li>
 *   <li>创建 node_modules 软链接</li>
 * </ol>
 *
 * @author Zriyo AI
 * @since 2026-03-01
 */
@Slf4j
public final class FrontendScaffoldUtils {

    private static final ConcurrentHashMap<String, ReentrantLock> INIT_LOCKS = new ConcurrentHashMap<>();
    private static final long LOCK_WAIT_SECONDS = 30L;
    private static final Pattern NPM_NAME_PATTERN = Pattern.compile("^[a-z0-9][a-z0-9._-]*$");

    private static final Set<String> ALLOW_ROOT_FILES = Set.of(
            ".env.example",
            ".eslintrc-auto-import.json",
            ".gitignore",
            ".npmrc",
            ".prettierrc.json",
            "README.md",
            "eslint.config.mjs",
            "index.html",
            "package-lock.json",
            "package.json",
            "postcss.config.js",
            "tailwind.config.js",
            "tsconfig.json",
            "tsconfig.node.json",
            "vite.config.ts"
    );

    private static final Set<String> ALLOW_RELATIVE_FILES = Set.of(
            "src/main.ts",
            "src/App.vue",
            "src/vite-env.d.ts",
            "src/types/env.d.ts",
            "src/assets/styles/variables.scss",
            "types/auto-imports.d.ts",
            "types/components.d.ts"
    );

    private FrontendScaffoldUtils() {
    }

    /**
     * 一次性执行初始化：复制最小模板 + 软链接 node_modules。
     *
     * @param scaffoldPath 脚手架目录
     * @param projectPath  目标项目目录
     * @param projectName  项目名称
     * @throws IOException 文件操作异常
     */
    public static void initProject(Path scaffoldPath, Path projectPath, String projectName) throws IOException {
        long start = System.currentTimeMillis();
        Path normalizedScaffold = normalizeAbsolutePath(scaffoldPath);
        Path normalizedProject = normalizeAbsolutePath(projectPath);
        String normalizedProjectName = normalizeProjectName(projectName);

        validatePaths(normalizedScaffold, normalizedProject);
        withProjectLock(normalizedProject, () -> {
            // 幂等：如果已经是目标状态，直接返回
            if (isAlreadyInitialized(normalizedProject, normalizedProjectName)) {
                log.info("Project already initialized, skip. project={}", normalizedProject);
                return;
            }

            Path parent = normalizedProject.getParent();
            if (parent == null) {
                throw new IOException("Project path has no parent: " + normalizedProject);
            }
            ensureDir(parent);

            Path stagePath = parent.resolve(normalizedProject.getFileName() + ".init-stage-" + System.nanoTime());
            Path backupPath = null;

            try {
                deleteIfExists(stagePath);
                copyMinimalTemplate(normalizedScaffold, stagePath, normalizedProjectName);
                linkNodeModules(normalizedScaffold, stagePath);

                if (Files.exists(normalizedProject) || Files.isSymbolicLink(normalizedProject)) {
                    backupPath = parent.resolve(normalizedProject.getFileName() + ".init-backup-" + System.nanoTime());
                    movePath(normalizedProject, backupPath);
                }

                movePath(stagePath, normalizedProject);

                if (backupPath != null) {
                    deleteIfExists(backupPath);
                }
            } catch (Exception e) {
                // 回滚：优先恢复旧目录
                deleteQuietly(stagePath);
                if (backupPath != null && (Files.exists(backupPath) || Files.isSymbolicLink(backupPath))) {
                    if (!Files.exists(normalizedProject) && !Files.isSymbolicLink(normalizedProject)) {
                        try {
                            movePath(backupPath, normalizedProject);
                        } catch (Exception rollbackEx) {
                            log.error("Rollback failed. backup={}, project={}, error={}",
                                    backupPath, normalizedProject, rollbackEx.getMessage(), rollbackEx);
                        }
                    }
                }
                if (e instanceof IOException ioException) {
                    throw ioException;
                }
                throw new IOException("Init project failed: " + e.getMessage(), e);
            }
        });

        log.info("Init project finished. project={}, costMs={}", normalizedProject, System.currentTimeMillis() - start);
    }

    /**
     * 复制最小模板文件并更新 package name。
     *
     * @param scaffoldPath 脚手架目录
     * @param projectPath  目标项目目录
     * @param projectName  项目名称
     * @throws IOException 文件操作异常
     */
    public static void copyMinimalTemplate(Path scaffoldPath, Path projectPath, String projectName) throws IOException {
        Path normalizedScaffold = normalizeAbsolutePath(scaffoldPath);
        Path normalizedProject = normalizeAbsolutePath(projectPath);
        String normalizedProjectName = normalizeProjectName(projectName);

        ensureDir(normalizedProject);

        for (String filename : ALLOW_ROOT_FILES) {
            Path src = normalizedScaffold.resolve(filename);
            Path dst = normalizedProject.resolve(filename);
            copyIfPresent(src, dst, "root");
        }

        for (String relPath : ALLOW_RELATIVE_FILES) {
            Path src = normalizedScaffold.resolve(relPath);
            Path dst = normalizedProject.resolve(relPath);
            copyIfPresent(src, dst, "relative");
        }

        Path packageJson = normalizedProject.resolve("package.json");
        if (Files.exists(packageJson)) {
            updatePackageName(packageJson, normalizedProjectName);
        }

        log.info("Scaffold copied to {} (rootFiles={}, relativeFiles={})",
                normalizedProject, ALLOW_ROOT_FILES.size(), ALLOW_RELATIVE_FILES.size());
    }

    /**
     * 创建 node_modules 软链接（project/node_modules -> scaffold/node_modules）。
     *
     * @param scaffoldPath 脚手架目录
     * @param projectPath  目标项目目录
     * @throws IOException 文件操作异常
     */
    public static void linkNodeModules(Path scaffoldPath, Path projectPath) throws IOException {
        Path normalizedScaffold = normalizeAbsolutePath(scaffoldPath);
        Path normalizedProject = normalizeAbsolutePath(projectPath);

        Path src = normalizedScaffold.resolve("node_modules");
        Path dst = normalizedProject.resolve("node_modules");

        if (!Files.exists(src)) {
            log.warn("node_modules source not found, skip symlink: {}", src);
            return;
        }

        deleteIfExists(dst);

        try {
            Path linkTarget = normalizedProject.relativize(src);
            Files.createSymbolicLink(dst, linkTarget);
            log.info("node_modules linked: {} -> {}", dst, linkTarget);
        } catch (UnsupportedOperationException | IOException | IllegalArgumentException e) {
            // 软链接失败自动降级复制，保障跨平台可用性（如 Windows 无管理员权限）
            log.warn("Create symlink failed, fallback to copy. dst={}, error={}", dst, e.getMessage());
            copyDirectory(src, dst);
            log.info("node_modules copied to {} (fallback mode)", dst);
        }
    }

    private static void copyIfPresent(Path src, Path dst, String tag) throws IOException {
        if (!Files.exists(src) || !Files.isRegularFile(src)) {
            log.warn("Template {} file missing, skipped: {}", tag, src);
            return;
        }
        ensureDir(dst.getParent());
        Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
    }

    private static void ensureDir(Path path) throws IOException {
        if (path != null && !Files.exists(path)) {
            Files.createDirectories(path);
        }
    }

    private static void updatePackageName(Path packageJson, String projectName) throws IOException {
        String content = Files.readString(packageJson, StandardCharsets.UTF_8);
        JSONObject obj = JSON.parseObject(content);
        if (obj == null) {
            return;
        }
        obj.put("name", projectName);
        String updated = JSON.toJSONString(obj, JSONWriter.Feature.PrettyFormat);
        Files.writeString(packageJson, updated, StandardCharsets.UTF_8);
    }

    private static boolean isAlreadyInitialized(Path projectPath, String projectName) throws IOException {
        if (!Files.exists(projectPath) || !Files.isDirectory(projectPath)) {
            return false;
        }

        Path packageJson = projectPath.resolve("package.json");
        if (!Files.exists(packageJson)) {
            return false;
        }

        String content = Files.readString(packageJson, StandardCharsets.UTF_8);
        JSONObject obj = JSON.parseObject(content);
        if (obj == null) {
            return false;
        }
        if (!projectName.equals(obj.getString("name"))) {
            return false;
        }

        for (String file : ALLOW_ROOT_FILES) {
            if (!Files.exists(projectPath.resolve(file))) {
                return false;
            }
        }
        for (String file : ALLOW_RELATIVE_FILES) {
            if (!Files.exists(projectPath.resolve(file))) {
                return false;
            }
        }

        Path nodeModules = projectPath.resolve("node_modules");
        return Files.exists(nodeModules) || Files.isSymbolicLink(nodeModules);
    }

    private static void copyDirectory(Path src, Path dst) throws IOException {
        if (!Files.exists(src) || !Files.isDirectory(src)) {
            return;
        }
        ensureDir(dst);
        Files.walkFileTree(src, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path targetDir = dst.resolve(src.relativize(dir));
                ensureDir(targetDir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path targetFile = dst.resolve(src.relativize(file));
                ensureDir(targetFile.getParent());
                Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void movePath(Path src, Path dst) throws IOException {
        ensureDir(dst.getParent());
        try {
            Files.move(src, dst, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(src, dst, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void deleteIfExists(Path path) throws IOException {
        if (!Files.exists(path) && !Files.isSymbolicLink(path)) {
            return;
        }
        if (Files.isSymbolicLink(path) || Files.isRegularFile(path)) {
            Files.deleteIfExists(path);
            return;
        }
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.deleteIfExists(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.deleteIfExists(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void deleteQuietly(Path path) {
        try {
            deleteIfExists(path);
        } catch (Exception e) {
            log.warn("Delete path quietly failed: {}, error={}", path, e.getMessage());
        }
    }

    private static Path normalizeAbsolutePath(Path path) {
        Objects.requireNonNull(path, "path cannot be null");
        return path.toAbsolutePath().normalize();
    }

    private static String normalizeProjectName(String projectName) {
        if (projectName == null) {
            throw new IllegalArgumentException("projectName cannot be null");
        }
        String normalized = projectName.trim().toLowerCase();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("projectName cannot be empty");
        }
        if (!NPM_NAME_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("projectName is not a valid npm name: " + projectName);
        }
        return normalized;
    }

    private static void validatePaths(Path scaffoldPath, Path projectPath) {
        if (!Files.exists(scaffoldPath) || !Files.isDirectory(scaffoldPath)) {
            throw new IllegalArgumentException("scaffoldPath not exists or not dir: " + scaffoldPath);
        }
        if (projectPath.getParent() == null) {
            throw new IllegalArgumentException("projectPath has no parent: " + projectPath);
        }
        if (projectPath.equals(scaffoldPath) || projectPath.startsWith(scaffoldPath)) {
            throw new IllegalArgumentException("projectPath cannot be inside scaffoldPath: " + projectPath);
        }
    }

    private static void withProjectLock(Path projectPath, IoRunnable runnable) throws IOException {
        String key = projectPath.toString();
        ReentrantLock lock = INIT_LOCKS.computeIfAbsent(key, k -> new ReentrantLock());
        boolean acquired = false;
        try {
            acquired = lock.tryLock(LOCK_WAIT_SECONDS, TimeUnit.SECONDS);
            if (!acquired) {
                throw new IOException("Acquire init lock timeout: " + projectPath);
            }
            runnable.run();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Acquire init lock interrupted: " + projectPath, e);
        } finally {
            if (acquired) {
                lock.unlock();
            }
            if (!lock.isLocked() && !lock.hasQueuedThreads()) {
                INIT_LOCKS.remove(key, lock);
            }
        }
    }

    @FunctionalInterface
    private interface IoRunnable {
        void run() throws IOException;
    }
}
