package com.zriyo.aicodemother.util;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.zriyo.aicodemother.model.AppConstant;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class ProjectDoctor {

    private static final String BUILD_OUTPUT_DIR = AppConstant.BUILD_OUTPUT_DIR;
    public static final String TMP_CODE_OUTPUT = AppConstant.TMP_DIR + "/" + AppConstant.APP_GEN_FILE_PATH + "/";

    // Playwright 实例（单例）
    private static final Playwright playwright;
    private static final Browser browser;

    static {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
    }

    /**
     * 挂载共享 node_modules（符号链接）
     */
    public static void mountSharedDependencies(String projectPath) throws IOException {
        Path masterPath = CodeOutputManager.getCodeOutputBaseDir()
                .resolve(AppConstant.SHARED_NODE_MODULES_SUBPATH);
        Path userLinkPath = Paths.get(projectPath, "node_modules");

        if (!Files.exists(masterPath)) {
            log.warn("公共依赖库缺失: {}", masterPath);
            return;
        }

        if (Files.exists(userLinkPath)) {
            try {
                if (Files.isSymbolicLink(userLinkPath)) {
                    Files.delete(userLinkPath);
                }
            } catch (Exception e) {
                log.warn("清理旧 node_modules 链接失败", e);
            }
        }

        if (!Files.exists(userLinkPath)) {
            Files.createSymbolicLink(userLinkPath, masterPath.toAbsolutePath());
        }
    }

    /**
     * 静态构建检查（Vite build）
     */
    public static DiagnosisResult runStaticDiagnosis(String projectPath) {
        if (projectPath == null || !Files.exists(Paths.get(projectPath))) {
            return DiagnosisResult.fail("ENV_ERROR", "项目路径无效: " + projectPath, null, 0);
        }

        try {
            mountSharedDependencies(projectPath);
        } catch (IOException e) {
            log.error("挂载依赖失败", e);
            return DiagnosisResult.fail("ENV_ERROR", "依赖挂载失败: " + e.getMessage(), "package.json", 0);
        }

        String buildOutput = runCommand(projectPath, "npm", "run", "build");

        // 检查构建是否包含错误关键词
        if (buildOutput != null && (buildOutput.contains("Error:") || buildOutput.contains("failed") || buildOutput.contains("Build failed") || buildOutput.contains("error during build"))) {
            String suspectedFile = extractFilePathFromLog(buildOutput);
            String lowerOutput = buildOutput.toLowerCase();

            // 🔥 增强逻辑：针对 Tailwind/PostCSS 的各种怪异报错，强制指向 global.css
            // 如果提取出的文件是 unknown，或者是 index.html (通常是代理样式报错)，且包含样式关键词
            boolean isStyleError = lowerOutput.contains("@apply") ||
                    lowerOutput.contains("@tailwind") ||
                    lowerOutput.contains("postcss") ||
                    lowerOutput.contains("circular dependency");

            if (isStyleError && ("unknown".equals(suspectedFile) || suspectedFile.endsWith("index.html"))) {
                suspectedFile = "src/styles/global.css";
                log.info("检测到样式构建错误，已强制修正目标文件为: {}", suspectedFile);
            }

            log.warn("构建失败 - 提取文件: '{}' | 项目路径: {}", suspectedFile, projectPath);
            return DiagnosisResult.fail("BUILD_ERROR", buildOutput, suspectedFile, 0);
        }
        return DiagnosisResult.pass();
    }

    /**
     * 运行时诊断（分级处理）
     */
    public static DiagnosisResult runRuntimeDiagnosis(String projectPath, String pageUrl) {
        if (projectPath == null || !Files.exists(Paths.get(projectPath))) {
            return DiagnosisResult.fail("SYSTEM_ERROR", "无效的项目路径: " + projectPath, null, 0);
        }

        try (Page page = browser.newPage()) {
            List<DiagnosisResult> warnings = new ArrayList<>();
            List<DiagnosisResult> errors = new ArrayList<>();

            page.onConsoleMessage(msg -> {
                String text = msg.text();
                if (text.contains("favicon")) return;

                // 忽略不可修复的网络/安全错误
                if (text.contains("Failed to load resource")
                        || text.contains("net::ERR_")
                        || text.contains("CORS")
                        || text.contains("tunnel connection failed")
                        || text.contains("Fetch API cannot load")
                        || text.contains("Blocked by Content Security Policy")
                        || text.contains("ERR_CONNECTION_")
                        || text.contains("ERR_CERT_")
                        || text.contains("NS_ERROR_")) {
                    log.debug("跳过不可修复的网络/安全错误: {}", text);
                    return;
                }

                if ("error".equals(msg.type())) {
                    LocationInfo loc = parseLocationString(msg.location());
                    DiagnosisResult result = DiagnosisResult.fail("RUNTIME_ERROR", "❌ [Console] " + text, loc.filePath, loc.lineNumber);

                    if (text.contains("is not defined") || text.contains("Cannot read properties")) {
                        errors.add(result);
                    } else {
                        warnings.add(result);
                    }
                }
            });

            page.onPageError(e -> {
                errors.add(DiagnosisResult.fail("PAGE_CRASH", "💥 " + e, "unknown", 0));
            });

            try {
                log.info("诊断页面: {} | 项目路径: {}", pageUrl, projectPath);
                page.navigate(pageUrl);
                page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE,
                        new Page.WaitForLoadStateOptions().setTimeout(AppConstant.PAGE_LOAD_TIMEOUT_MS));
                Thread.sleep(AppConstant.POST_LOAD_SLEEP_MS);
            } catch (Exception e) {
                log.warn("页面加载异常（可能已捕获 JS 错误）", e);
            }

            if (!errors.isEmpty()) {
                DiagnosisResult first = errors.get(0);
                String enhancedFile = locateSourceFileByContext(projectPath, first.errorFilePath, first.message, null);
                if (!"unknown".equals(enhancedFile)) {
                    first.errorFilePath = enhancedFile;
                    first.lineNumber = 0;
                    first.message += "\n   👉 [AI定位] 源文件: " + enhancedFile;
                }
                return first;
            }

            if (!warnings.isEmpty()) {
                warnings.forEach(w -> log.warn("运行时警告: {}", w.message));
            }

            return DiagnosisResult.pass();

        } catch (Exception e) {
            log.error("运行时诊断器异常", e);
            return DiagnosisResult.fail("SYSTEM_ERROR", "诊断器内部异常: " + e.getMessage(), null, 0);
        }
    }

    // ==================== 核心工具方法 ====================

    public static String locateSourceFileByContext(String projectRoot, String errorFilePath, String errorMsg, String stackTrace) {
        if (errorFilePath != null && (errorFilePath.endsWith(".js") || errorFilePath.contains("index-") || errorFilePath.contains("/assets/"))) {
            String jsName = new File(errorFilePath).getName().split("\\?")[0];
            Path assetsPath = Paths.get(projectRoot, BUILD_OUTPUT_DIR, AppConstant.BUILD_ASSETS_DIR, jsName);
            Path rootJsPath = Paths.get(projectRoot, BUILD_OUTPUT_DIR, jsName);

            String vueFile = "unknown";
            if (Files.exists(assetsPath)) {
                vueFile = locateOriginalVueFile(assetsPath.toString(), 0);
            } else if (Files.exists(rootJsPath)) {
                vueFile = locateOriginalVueFile(rootJsPath.toString(), 0);
            }
            if (!"unknown".equals(vueFile)) return vueFile.replace("\\", "/");
        }

        String stackMatch = findComponentInStackTrace(stackTrace);
        if (stackMatch != null) {
            String fuzzyMatch = locateSourceFileByErrorSnippet(projectRoot, stackMatch);
            if (!"unknown".equals(fuzzyMatch)) return fuzzyMatch;
        }

        String sourceMatch = locateSourceFileByErrorSnippet(projectRoot, errorMsg);
        if (!"unknown".equals(sourceMatch)) return sourceMatch;

        if (errorFilePath != null && errorFilePath.endsWith(".vue")) return errorFilePath;
        log.warn("无法定位逻辑错误源文件，报错路径: {}", errorFilePath);
        return "unknown";
    }

    private static String findComponentInStackTrace(String stack) {
        if (stack == null || stack.isEmpty()) return null;
        Pattern p = Pattern.compile("(?:at\\s+)?([A-Z][a-zA-Z0-9]+)(?:\\s+\\(|\\.|:|$)");
        Matcher m = p.matcher(stack);
        if (m.find()) return m.group(1);
        return null;
    }

    /**
     * 🔥 增强版日志文件路径提取
     * 专门处理 Vite file: 行，以及 html-proxy 代理文件
     */
    public static String extractFilePathFromLog(String log) {
        if (log == null || log.isEmpty()) return "unknown";
        // 清理 ANSI 颜色码、换行符等
        String cleanLog = log.replaceAll("\u001B\\[[;\\d]*m", "").replaceAll("\r\n|\r|\n", " ").replace("\\", "/");

        // 🎯 0. 优先拦截：Vite HTML Proxy / Inline CSS 错误
        // 这种错误通常形如 index.html?html-proxy&inline-css...
        // 且伴随着 tailwind/postcss 错误。此时修复点往往在全局 CSS 文件中。
        boolean isHtmlProxy = cleanLog.contains("html-proxy") || cleanLog.contains("inline-css");
        boolean isCssError = cleanLog.contains("tailwind") || cleanLog.contains("postcss") || cleanLog.contains("@layer");

        if (isHtmlProxy && isCssError) {
            // 这里返回一个约定俗成的全局样式文件路径，让 AI 去这里补充指令
            // 如果你的项目结构不同，这里可以改为 "src/style.css"
            return "src/styles/global.css";
        }

        // 1. 优先匹配 Vite 的 "file: ..." 行
        Pattern pViteFile = Pattern.compile("file:\\s*([a-zA-Z]:[^\\r\\n]*|/[^\\r\\n]*?/([^/\\r\\n]+?\\.(?:css|scss|less|vue|js|ts)))", Pattern.CASE_INSENSITIVE);
        Matcher mVite = pViteFile.matcher(cleanLog);
        if (mVite.find()) {
            String fullPath = mVite.group(1);

            // 如果 file 指向的是 html-proxy 乱七八糟的路径，直接截断判断
            if (fullPath.contains("html-proxy")) {
                return "src/styles/global.css"; // 再次兜底
            }

            // 尝试提取相对路径
            int srcIdx = fullPath.indexOf("/src/");
            int stylesIdx = fullPath.indexOf("/styles/");
            if (srcIdx >= 0) {
                return fullPath.substring(srcIdx + 1);
            } else if (stylesIdx >= 0) {
                return fullPath.substring(stylesIdx + 1);
            } else {
                return new File(fullPath).getName();
            }
        }

        // 2. HTML 错误
        Pattern pHtmlError = Pattern.compile("(?:^|\\s)at\\s+((?:[a-zA-Z]:)?[^:\\s]*?/index\\.html):(\\d+):(\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher mHtml = pHtmlError.matcher(cleanLog);
        if (mHtml.find()) {
            String fullPath = mHtml.group(1);
            int lastSlash = fullPath.lastIndexOf('/');
            return lastSlash >= 0 ? fullPath.substring(lastSlash + 1) : fullPath;
        }

        // 3. 模块缺失
        Pattern pModuleNotFound = Pattern.compile("Module not found: Error: Can't resolve '([^']+)' in '([^']+)'", Pattern.CASE_INSENSITIVE);
        Matcher mModule = pModuleNotFound.matcher(cleanLog);
        if (mModule.find()) {
            return "package.json";
        }

        // 4. 标准 src/... 路径匹配
        Pattern[] patterns = {
                Pattern.compile("(src/[^?\\s:\\n]+\\.(?:vue|js|jsx|ts|tsx|css|scss|less))(?:\\?[^:\\n]*)?:(\\d+):(\\d+)", Pattern.CASE_INSENSITIVE),
                Pattern.compile("(src/[^\\s?:]+\\.(?:vue|js|jsx|ts|tsx|css|scss|less)):(\\d+):(\\d+)", Pattern.CASE_INSENSITIVE),
                Pattern.compile("file:\\s*(?:/|[a-zA-Z]:/)?(?:[^\\s]*/)?(src/[^?\\s:]+\\.(?:vue|js|ts))", Pattern.CASE_INSENSITIVE),
                Pattern.compile("(src/[a-zA-Z0-9_\\-.\\/]+\\.(?:vue|js|ts|jsx|tsx))", Pattern.CASE_INSENSITIVE)
        };
        for (Pattern p : patterns) {
            Matcher m = p.matcher(cleanLog);
            if (m.find()) return m.group(1);
        }

        return "unknown";
    }

    public static String locateOriginalVueFile(String jsFilePath, int errorLine) {
        if (errorLine <= 0) return "unknown";
        try {
            Path path = Paths.get(jsFilePath);
            if (!Files.exists(path)) return "unknown";
            List<String> lines = Files.readAllLines(path);
            Pattern p = Pattern.compile("\"__file\"\\s*:\\s*\"([^\"]+\\.vue)\"");
            int start = Math.max(0, errorLine - 200);
            int end = Math.min(lines.size(), errorLine + 50);
            for (int i = end - 1; i >= start; i--) {
                Matcher m = p.matcher(lines.get(i));
                if (m.find()) return m.group(1).replace("\\", "/");
            }
        } catch (Exception e) {
            log.debug("从 JS 文件定位 Vue 源文件失败", e);
        }
        return "unknown";
    }

    public static String locateSourceFileByErrorSnippet(String projectPath, String errorMessage) {
        String snippet = extractCodeSnippet(errorMessage);
        if (snippet.isEmpty() || snippet.length() < 2) return "unknown";

        log.debug("在源码中搜索关键词: [{}]", snippet);
        try {
            Path srcDir = Paths.get(projectPath, "src");
            if (!Files.exists(srcDir)) return "unknown";

            AtomicReference<String> bestMatch = new AtomicReference<>("unknown");
            try (Stream<Path> paths = Files.walk(srcDir)) {
                paths.filter(Files::isRegularFile)
                        .filter(p -> p.toString().matches(".*\\.(vue|js|ts)$"))
                        .forEach(p -> {
                            if (!"unknown".equals(bestMatch.get()) && bestMatch.get().endsWith(".vue")) return;
                            try {
                                String content = Files.readString(p);
                                if (content.contains(snippet)) {
                                    String rel = "src/" + srcDir.relativize(p.toAbsolutePath()).toString().replace("\\", "/");
                                    bestMatch.set(rel);
                                    if (rel.endsWith(".vue")) throw new RuntimeException("FOUND_VUE");
                                }
                            } catch (IOException ignored) {}
                        });
            } catch (RuntimeException e) {
                if ("FOUND_VUE".equals(e.getMessage())) return bestMatch.get();
            }
            return bestMatch.get();
        } catch (Exception e) {
            log.warn("关键词搜索失败", e);
            return "unknown";
        }
    }

    private static String extractCodeSnippet(String errorMsg) {
        if (errorMsg == null) return "";
        if (errorMsg.contains("renderList")) {
            log.debug("检测到 Vue renderList 报错，正在提取属性关键词...");
        }

        Pattern[] patterns = {
                Pattern.compile("([a-zA-Z0-9_$.]+)\\.(?:\\w+)\\s+is\\s+not\\s+a\\s+function", Pattern.CASE_INSENSITIVE),
                Pattern.compile("reading\\s+'([^']+)'"),
                Pattern.compile("property\\s+'([^']+)'\\s+of\\s+undefined"),
                Pattern.compile("property\\s+'([^']+)'\\s+of\\s+null"),
                Pattern.compile("([a-zA-Z0-9_$]+)\\s+is\\s+not\\s+defined"),
                Pattern.compile("assignment\\s+to\\s+constant\\s+variable", Pattern.CASE_INSENSITIVE)
        };
        for (Pattern p : patterns) {
            Matcher m = p.matcher(errorMsg);
            if (m.find()) {
                if (m.groupCount() >= 1) {
                    return cleanVuePrefix(m.group(1));
                }
            }
        }
        return "";
    }

    private static String cleanVuePrefix(String expr) {
        if (expr == null) return "";
        return expr.replace("__props.", "")
                .replace("_ctx.", "")
                .replace("$setup.", "")
                .replace(".value", "")
                .replace("_unref(", "")
                .replace("_toDisplayString(", "")
                .replaceAll("\\)$", "")
                .replaceAll(".*\\.(\\w+)$", "$1");
    }

    // ==================== 辅助方法 ====================

    private static LocationInfo parseLocationString(String s) {
        if (s == null || s.isEmpty()) return new LocationInfo("unknown", 0);
        Pattern p = Pattern.compile("^(.*):(\\d+):(\\d+)$");
        Matcher m = p.matcher(s);
        if (m.find()) return new LocationInfo(cleanPath(m.group(1)), Integer.parseInt(m.group(2)));
        return new LocationInfo(cleanPath(s), 0);
    }

    private static String cleanPath(String url) {
        if (url == null) return "unknown";
        if (url.contains("/")) url = url.substring(url.lastIndexOf("/") + 1);
        return url.split("\\?")[0];
    }

    private static String runCommand(String dir, String... cmd) {
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(new File(dir));
            pb.redirectErrorStream(true);
            Process p = pb.start();
            String out = new BufferedReader(new InputStreamReader(p.getInputStream()))
                    .lines().collect(Collectors.joining("\n"));
            if (!p.waitFor(60, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                return "Command timeout after 60s";
            }
            return p.exitValue() == 0 ? null : out;
        } catch (Exception e) {
            return "Command execution error: " + e.getMessage();
        }
    }

    // ==================== 内部类 ====================

    private static class LocationInfo {
        String filePath;
        int lineNumber;
        LocationInfo(String f, int l) {
            this.filePath = f != null ? f : "unknown";
            this.lineNumber = l;
        }
    }

    @Data
    public static class DiagnosisResult {
        public boolean success;
        public String phase;
        public String message;
        public String errorFilePath;
        public Integer lineNumber;

        public static DiagnosisResult pass() {
            DiagnosisResult r = new DiagnosisResult();
            r.success = true;
            r.phase = "SUCCESS";
            r.message = "OK";
            r.errorFilePath = null;
            r.lineNumber = 0;
            return r;
        }

        public static DiagnosisResult fail(String phase, String message, String errorFilePath, int lineNumber) {
            DiagnosisResult r = new DiagnosisResult();
            r.success = false;
            r.phase = phase;
            r.message = message != null ? message : "Unknown error";
            r.errorFilePath = errorFilePath != null ? errorFilePath : "unknown";
            r.lineNumber = lineNumber;
            return r;
        }
    }
}
