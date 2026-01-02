package com.zriyo.aicodemother.ai.tools;

import com.anji.captcha.util.StringUtils;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class CodeWriteTool {

    private final String fullRelativePath;

    private final AtomicInteger writeFileCount = new AtomicInteger(0);
    private final AtomicInteger continueWriteCount = new AtomicInteger(0);
    private static final int MAX_WRITE_FILE = 1;
    private static final int MAX_CONTINUE_WRITE = 1;

    private static final Pattern CONTENT_PATTERN =
            Pattern.compile("\"content\"\\s*:\\s*\"([\\s\\S]*?)\"", Pattern.DOTALL);

    public CodeWriteTool(String fullRelativePath) {
        if (StringUtils.isBlank(fullRelativePath)) {
            throw new IllegalArgumentException("文件路径不能为空");
        }
        this.fullRelativePath = fullRelativePath.startsWith("/")
                ? fullRelativePath.substring(1)
                : fullRelativePath;
    }

    /**
     * 新增：修复终结工具
     * 作用：作为 AI 逻辑的“句号”，防止其在写入成功后因无事可做而反复调用写入工具。
     */
    @Tool("完成修复并退出")
    public String finishRepair(@P("简述修复了什么问题") String summary) {
        log.info("模型报告任务完成: {}, 总结: {}", fullRelativePath, summary);
        return "{\"status\":\"COMPLETED\",\"message\":\"修复任务正式结束，请立即停止生成。\"}";
    }

    @Tool("代码内容首次写入工具")
    public String writeFile(@P(value = "此次操作描述", required = false) String description,
                            @P(value = "文件内容", required = false) String content) {
        if (StringUtils.isBlank(content)) {
            return "请重新调用[代码内容首次写入工具]";
        }

        if (writeFileCount.get() >= MAX_WRITE_FILE) {
            log.warn("检测到重复调用 writeFile: {}", fullRelativePath);
            // 修改返回值：明确告知下一步必须调 finishRepair
            return "{\"error\":\"DUPLICATE_CALL\",\"message\":\"本轮已执行过写入操作。严禁重复调用。请立即调用 finishRepair 结束任务。\"}";
        }

        log.info("开始写入文件: {}, 描述: {}", fullRelativePath, description);

        try {
            content = recoverIfTruncated(description, content);
            Path outputPath = getOutputPath();

            String existing = readSafely(outputPath);
            if (content.equals(existing)) {
                log.info("文件内容无变化，拒绝写入: {}", fullRelativePath);
                writeFileCount.incrementAndGet();
                return "{\"error\":\"NO_CHANGES\",\"message\":\"提交的内容与原文件一致。请检查修复逻辑（如引号转义、括号闭合等）并重新尝试。\"}";
            }

            createParentDirs(outputPath);
            writeSafely(outputPath, content, false);

            writeFileCount.incrementAndGet();
            continueWriteCount.set(0);

            if (Objects.isNull(description) || content.length() > 2000) {
                return "{\"status\":\"PARTIAL_SUCCESS\",\"message\":\"内容已部分写入。请调用 continueWriting 进行续写。\"}";
            }

            log.info("文件写入成功: {}", outputPath);
            // 修改返回值：引导 AI 走向 finishRepair 而不是闭嘴
            return String.format("{\"status\":\"SUCCESS\",\"file\":\"%s\",\"next_step\":\"MUST_CALL_finishRepair_TO_EXIT\"}", fullRelativePath);

        } catch (Exception e) {
            log.error("文件写入失败: {}", fullRelativePath, e);
            return "{\"status\":\"FAILED\",\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    @Tool("代码截断续写工具")
    public String continueWriting(@P("此次操作描述") String description,
                                  @P(value = "追加内容", required = true) String content) {
        if (StringUtils.isBlank(content)) {
            return "SKIPPED_EMPTY_INPUT";
        }

        if (continueWriteCount.get() >= MAX_CONTINUE_WRITE) {
            return "{\"error\":\"MAX_LIMIT\",\"message\":\"续写次数已达上限。请直接调用 finishRepair 退出。\"}";
        }

        try {
            content = recoverIfTruncated(description, content);
            Path outputPath = getOutputPath();
            createParentDirs(outputPath);

            String existing = readSafely(outputPath);
            content = trimOverlap(existing, content);

            writeSafely(outputPath, content, true);

            continueWriteCount.incrementAndGet();
            log.info("文件续写成功: {}", outputPath);
            return String.format("{\"status\":\"SUCCESS\",\"action\":\"APPENDED\",\"next_step\":\"MUST_CALL_finishRepair_TO_EXIT\"}");

        } catch (Exception e) {
            log.error("文件续写失败: {}", fullRelativePath, e);
            return "{\"status\":\"FAILED\",\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    // ========= 以下保持原有逻辑不变 =========

    private String recoverIfTruncated(String description, String content) {
        if (StringUtils.isNotBlank(description)) {
            return content;
        }
        try {
            Matcher cm = CONTENT_PATTERN.matcher(content);
            if (cm.find()) {
                return unescapeJsonString(cm.group(1));
            }
        } catch (Exception ignore) {}
        return content;
    }

    private String unescapeJsonString(String s) {
        if (s == null) return null;
        return s.replace("\\n", "\n")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    private String trimOverlap(String existing, String newContent) {
        if (existing == null || existing.isEmpty() || newContent.isEmpty()) {
            return newContent;
        }
        int window = Math.min(existing.length(), 8000);
        String tail = existing.substring(existing.length() - window);
        int max = Math.min(tail.length(), newContent.length());
        for (int i = max; i > 0; i--) {
            if (newContent.startsWith(tail.substring(tail.length() - i))) {
                return newContent.substring(i);
            }
        }
        return newContent;
    }

    private String readSafely(Path path) {
        try {
            if (!Files.exists(path)) return "";
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    private void writeSafely(Path path, String data, boolean append) throws IOException {
        if (append) {
            Files.write(path, data.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } else {
            Files.write(path, data.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
    }

    private void createParentDirs(Path path) throws IOException {
        Path parent = path.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
    }

    private Path getOutputPath() {
        return Paths.get("tmp", "code_output", fullRelativePath);
    }
}
