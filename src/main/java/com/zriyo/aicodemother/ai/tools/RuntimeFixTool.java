package com.zriyo.aicodemother.ai.tools;

import com.anji.captcha.util.StringUtils;
import com.zriyo.aicodemother.model.AppConstant;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class RuntimeFixTool {

    private final Long appId;
    // 每一个 Tool 实例都是随 AI 服务创建的，任务结束随对象销毁，彻底解决内存泄露
    private final ConcurrentHashMap<String, Integer> writeFileCountMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> continueWriteCountMap = new ConcurrentHashMap<>();

    private static final int MAX_WRITE_FILE = 1;
    private static final int MAX_CONTINUE_WRITE = 2;

    private static final Pattern CONTENT_PATTERN =
            Pattern.compile("\"content\"\\s*:\\s*\"([\\s\\S]*?)\"", Pattern.DOTALL);

    public RuntimeFixTool(Long appId) {
        this.appId = appId;
    }

    private String getOutputPath() {
        return AppConstant.TMP_DIR + "/" + AppConstant.APP_GEN_FILE_PATH + "/" + AppConstant.VUE_PROJECT_PREFIX + appId;
    }

    @Tool("完成修复并退出任务")
    public String finishRepair(@P("修复总结") String summary) {
        log.info("【任务完成】AppId: {} | 总结: {}", appId, summary);
        return "{\"status\":\"COMPLETED\"}";
    }

    @Tool("代码内容写入工具")
    public String writeFile(@P("文件相对路径") String relativePath,
                            @P(value = "操作描述", required = false) String description,
                            @P(value = "完整代码内容", required = false) String content) {
        if (StringUtils.isBlank(content)) return "Error: 内容不能为空";

        String cleanPath = normalizePath(relativePath);

        // --- 线程安全处理：Check-then-Act 原子化 ---
        synchronized (writeFileCountMap) {
            int count = writeFileCountMap.getOrDefault(cleanPath, 0);
            if (count >= MAX_WRITE_FILE) {
                return "{\"error\":\"DUPLICATE_CALL\",\"message\":\"该文件已写入过，禁止重复调用。\"}";
            }
            writeFileCountMap.put(cleanPath, count + 1);
        }

        try {
            String processedContent = recoverIfTruncated(description, content);
            Path outputPath = Paths.get(getOutputPath(), cleanPath).toAbsolutePath().normalize();

            // --- 文件级并发锁：防止多个线程同时写同一个物理文件 ---
            synchronized (cleanPath.intern()) {
                createParentDirs(outputPath);
                Files.writeString(outputPath, processedContent, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                log.info("【writeFile 成功】AppId: {} | 路径: {} | 长度: {}", appId, cleanPath, processedContent.length());
            }

            continueWriteCountMap.put(cleanPath, 0);
            return "{\"status\":\"SUCCESS\",\"file\":\"" + cleanPath + "\"}";
        } catch (Exception e) {
            log.error("写入失败", e);
            return "{\"status\":\"FAILED\",\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    @Tool("代码截断续写工具")
    public String continueWriting(@P("文件相对路径") String relativePath,
                                  @P("操作描述") String description,
                                  @P(value = "追加的代码片段") String content) {
        if (StringUtils.isBlank(content)) return "Error: 内容不能为空";

        String cleanPath = normalizePath(relativePath);

        synchronized (continueWriteCountMap) {
            int count = continueWriteCountMap.getOrDefault(cleanPath, 0);
            if (count >= MAX_CONTINUE_WRITE) return "{\"error\":\"MAX_LIMIT\"}";
            continueWriteCountMap.put(cleanPath, count + 1);
        }

        try {
            String processedContent = recoverIfTruncated(description, content);
            Path outputPath = Paths.get(getOutputPath(), cleanPath).toAbsolutePath().normalize();

            synchronized (cleanPath.intern()) {
                String existing = Files.exists(outputPath) ? Files.readString(outputPath, StandardCharsets.UTF_8) : "";
                String appendData = trimOverlap(existing, processedContent);
                Files.writeString(outputPath, appendData, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
                log.info("【continueWriting 成功】AppId: {} | 路径: {} | 追加长度: {}", appId, cleanPath, appendData.length());
            }
            return "{\"status\":\"SUCCESS\"}";
        } catch (Exception e) {
            return "{\"status\":\"FAILED\",\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    // --- 内部逻辑辅助 ---
    private String normalizePath(String path) {
        if (StringUtils.isBlank(path)) return "";
        String p = path.trim().replace("@/", "src/");
        if (p.startsWith("/") || p.startsWith("\\")) p = p.substring(1);
        return p;
    }

    private String recoverIfTruncated(String description, String content) {
        String res = content;
        if (StringUtils.isBlank(description)) {
            try {
                Matcher cm = CONTENT_PATTERN.matcher(content);
                if (cm.find()) res = cm.group(1);
            } catch (Exception ignore) {}
        }
        // 恢复转义
        return res.replace("$ {", "${").replace("{ {", "{{").replace("} }", "}}");
    }

    private String trimOverlap(String existing, String newContent) {
        if (StringUtils.isBlank(existing)) return newContent;
        int checkLen = Math.min(existing.length(), 300);
        String tail = existing.substring(existing.length() - checkLen);
        for (int i = checkLen; i > 0; i--) {
            if (newContent.startsWith(tail.substring(tail.length() - i))) {
                return newContent.substring(i);
            }
        }
        return newContent;
    }

    private void createParentDirs(Path path) throws IOException {
        Path parent = path.getParent();
        if (parent != null && !Files.exists(parent)) Files.createDirectories(parent);
    }
}
