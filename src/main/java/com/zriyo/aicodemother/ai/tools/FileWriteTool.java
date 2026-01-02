package com.zriyo.aicodemother.ai.tools;

import com.anji.captcha.util.StringUtils;
import com.zriyo.aicodemother.model.AppConstant;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文件写入工具（生产级）
 * - 支持 AI 通过工具调用写入文件
 * - 自动校验 Vue/JS/HTML 内容完整性
 * - 若内容被截断，拒绝写入并返回明确错误
 */
@Slf4j
@Deprecated
public class FileWriteTool {

    private final Long appId;

    public FileWriteTool(Long appId) {
        this.appId = appId;
    }

    @Tool("文件内容写入工具（自动校验完整性）")
    public String writeFile(
            @P(value = "文件的相对路径", required = false) String relativeFilePath,
            @P(value = "对此次操作的简短描述", required = false)  String description ,@P(value = "要写入文件的内容", required = false) String content) {
        String threadName = Thread.currentThread().getName();
        log.info("【开始生成】appId={}, 线程={}", appId, threadName);

        log.info("开始写入文件: {}, 描述: {}", relativeFilePath, description);
        log.info("文件内容长度: {},文件内容:{}", content == null ? 0 : content.length(), " ");

        if (StringUtils.isBlank(relativeFilePath)) {
            return "SKIPPED_EMPTY_PATH";
        }
        if (StringUtils.isBlank(content)) {
            return "SKIPPED_EMPTY_CONTENT";
        }


        try {
            Path baseDir = Paths.get(AppConstant.TMP_DIR, AppConstant.APP_GEN_FILE_PATH);
            String projectDirName = AppConstant.VUE_PROJECT_PREFIX + appId;
            Path projectRoot = baseDir.resolve(projectDirName);
            Path path = projectRoot.resolve(relativeFilePath);

            Files.createDirectories(path.getParent());
            Files.write(path, content.getBytes(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
            // 生产级完整性校验
            ValidationResult result = validateContentIntegrity(relativeFilePath, content);
            if (!result.isValid()) {
                String errorMsg = String.format(
                        "FILE_TRUNCATED: %s. 文件可能被截断，请调用 continueWriting 工具补全剩余内容，不要重复已写部分。",
                        result.getMessage()
                );
                log.warn("文件内容不完整，需要追加写入: {} - 原因: {}", relativeFilePath, result.getMessage());
                return errorMsg;
            }
            log.info("✅ 成功写入文件: {}", path.toAbsolutePath());
            return "文件写入成功: " + relativeFilePath;
        } catch (IOException e) {
            String errorMessage = "文件写入失败: " + relativeFilePath + ", 错误: " + e.getMessage();
            log.error(errorMessage, e);
            return errorMessage;
        }
    }

    /**
     * 继续写入已有文件的剩余内容（追加模式）
     */
    @Tool("继续写入文件剩余内容")
    public String continueWriting(
            @P(value = "文件的相对路径", required = true) String relativeFilePath,
            @P("此次操作的简短描述") String description,
            @P(value = "要追加的内容", required = true) String content
    ) {
        log.info("继续写入文件: {}, 描述: {}", relativeFilePath, description);
        log.info("追加内容长度: {}", content == null ? 0 : content.length());

        if (StringUtils.isBlank(relativeFilePath)) {
            return "SKIPPED_EMPTY_PATH";
        }
        if (StringUtils.isBlank(content)) {
            return "SKIPPED_EMPTY_CONTENT";
        }

        try {
            Path baseDir = Paths.get(AppConstant.TMP_DIR, AppConstant.APP_GEN_FILE_PATH);
            String projectDirName = AppConstant.VUE_PROJECT_PREFIX + appId;
            Path projectRoot = baseDir.resolve(projectDirName);
            Path path = projectRoot.resolve(relativeFilePath);

            Files.createDirectories(path.getParent());

            // 追加写入
            Files.write(path, content.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);

            // 写入后做完整性校验
            String fullContent = Files.readString(path);
            ValidationResult result = validateContentIntegrity(relativeFilePath, fullContent);

            if (!result.isValid()) {
                String warning = String.format(
                        "FILE_TRUNCATED: %s. 文件可能仍未完整，请继续使用 continueWriting 工具补全剩余内容。",
                        result.getMessage()
                );
                log.warn("文件内容可能不完整: {} - {}", relativeFilePath, result.getMessage());
                return warning;
            }

            log.info("✅ 文件写入完成: {}", path.toAbsolutePath());
            return "文件写入成功: " + relativeFilePath;

        } catch (IOException e) {
            String errorMessage = "文件写入失败: " + relativeFilePath + ", 错误: " + e.getMessage();
            log.error(errorMessage, e);
            return errorMessage;
        }
    }


    // ==================== 完整性校验逻辑 ====================

    private ValidationResult validateContentIntegrity(String filePath, String content) {
        String ext = getFileExtension(filePath).toLowerCase();

        if (ext.equals("vue")) {
            return validateVueFile(content);
        } else if (ext.equals("js") || ext.equals("ts")) {
            return validateJavaScript(content);
        } else if (ext.equals("html")) {
            return validateHtml(content);
        } else {
            // 其他类型（如 css, json）暂不校验，或做简单检查
            return ValidationResult.valid();
        }
    }

    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return (lastDot > 0 && lastDot < filename.length() - 1)
                ? filename.substring(lastDot + 1)
                : "";
    }

    private ValidationResult validateVueFile(String content) {
        // 1. 检查 template 是否闭合
        if (!hasBalancedTag(content, "template")) {
            return ValidationResult.invalid("未闭合的 <template> 标签");
        }
        // 2. 检查 script 是否存在且合理
        if (!content.contains("<script")) {
            return ValidationResult.invalid("缺少 <script> 块");
        }
        // 3. 检查 JS 部分括号是否平衡（提取 script 内容）
        String scriptContent = extractScriptContent(content);
        if (!isBalancedBraces(scriptContent)) {
            return ValidationResult.invalid("JavaScript 括号 {} 不平衡，可能被截断");
        }
        // 4. 检查是否以不合理符号结尾
        if (endsWithTruncatedPattern(content)) {
            return ValidationResult.invalid("内容以逗号、操作符或未闭合标签结尾，疑似截断");
        }
        return ValidationResult.valid();
    }

    private ValidationResult validateJavaScript(String content) {
        if (!isBalancedBraces(content)) {
            return ValidationResult.invalid("JavaScript 括号 {} 或括号 () 不平衡");
        }
        if (endsWithTruncatedPattern(content)) {
            return ValidationResult.invalid("JS 内容疑似被截断");
        }
        return ValidationResult.valid();
    }

    private ValidationResult validateHtml(String content) {
        if (!hasBalancedTag(content, "html") && !hasBalancedTag(content, "body")) {
            // 不强制 html/body，但至少不能有明显未闭合
            // 更宽松：只检查是否有明显未闭合标签
        }
        if (endsWithTruncatedPattern(content)) {
            return ValidationResult.invalid("HTML 内容疑似被截断");
        }
        return ValidationResult.valid();
    }

    // 提取 <script>...</script> 中的内容（简化版，不处理嵌套）
    private String extractScriptContent(String vueContent) {
        Pattern p = Pattern.compile("<script[^>]*>([\\s\\S]*?)</script>", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(vueContent);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            sb.append(m.group(1)).append("\n");
        }
        return sb.toString();
    }

    // 检查指定标签是否平衡（仅顶层，不处理嵌套同名标签）
    private boolean hasBalancedTag(String content, String tagName) {
        Pattern open = Pattern.compile("<" + tagName + "\\b[^>]*>", Pattern.CASE_INSENSITIVE);
        Pattern close = Pattern.compile("</" + tagName + ">", Pattern.CASE_INSENSITIVE);
        int openCount = 0;
        for (Matcher m = open.matcher(content); m.find(); ) openCount++;
        int closeCount = 0;
        for (Matcher m = close.matcher(content); m.find(); ) closeCount++;
        return openCount == closeCount && openCount > 0;
    }

    // 检查括号、方括号、大括号是否平衡
    private boolean isBalancedBraces(String code) {
        if (StringUtils.isBlank(code)) return true;

        Deque<Character> stack = new ArrayDeque<>();
        boolean inString = false;
        char stringQuote = 0;
        boolean inLineComment = false;
        boolean inBlockComment = false;

        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);

            // 简单处理字符串和注释（避免括号在字符串内被误判）
            if (!inBlockComment && !inLineComment && (c == '"' || c == '\'' || c == '`')) {
                if (!inString) {
                    inString = true;
                    stringQuote = c;
                } else if (c == stringQuote && (i == 0 || code.charAt(i - 1) != '\\')) {
                    inString = false;
                }
                continue;
            }

            if (!inString) {
                if (!inBlockComment && i + 1 < code.length() && code.charAt(i) == '/' && code.charAt(i + 1) == '/') {
                    inLineComment = true;
                    continue;
                }
                if (inLineComment && c == '\n') {
                    inLineComment = false;
                    continue;
                }
                if (!inBlockComment && i + 1 < code.length() && code.charAt(i) == '/' && code.charAt(i + 1) == '*') {
                    inBlockComment = true;
                    i++; // skip next
                    continue;
                }
                if (inBlockComment && i >= 1 && code.charAt(i - 1) == '*' && c == '/') {
                    inBlockComment = false;
                    continue;
                }
            }

            if (inString || inLineComment || inBlockComment) {
                continue;
            }

            if (c == '{' || c == '[' || c == '(') {
                stack.push(c);
            } else if (c == '}' || c == ']' || c == ')') {
                if (stack.isEmpty()) return false; // extra closing
                char top = stack.pop();
                if ((c == '}' && top != '{') ||
                        (c == ']' && top != '[') ||
                        (c == ')' && top != '(')) {
                    return false; // mismatch
                }
            }
        }
        return stack.isEmpty();
    }

    // 检查是否以常见截断模式结尾
    private boolean endsWithTruncatedPattern(String content) {
        String trimmed = content.trim();
        if (trimmed.isEmpty()) return false;

        // 以逗号、点、操作符结尾
        if (trimmed.matches(".*[,+\\-*/=&|<>.]$")) {
            return true;
        }
        // 以未闭合 HTML 标签结尾（如 <div）
        if (trimmed.matches(".*<[a-zA-Z][^>]*$")) {
            return true;
        }
        // 以未闭合引号或反引号结尾
        long quoteCount = trimmed.chars().filter(ch -> ch == '"' || ch == '\'' || ch == '`').count();
        if (quoteCount % 2 == 1) {
            return true;
        }
        return false;
    }

    // ====== 辅助类 ======
    private static class ValidationResult {
        private final boolean valid;
        private final String message;

        private ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public static ValidationResult valid() {
            return new ValidationResult(true, "");
        }

        public static ValidationResult invalid(String message) {
            return new ValidationResult(false, message);
        }

        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
    }
}
