package com.zriyo.aicodemother.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
public class VueTemplateSanitizer {

    // HTML/SVG 中的 void（自闭合）元素，不能有结束标签
    private static final Set<String> VOID_TAGS = Set.of(
            "area", "base", "br", "col", "embed", "hr", "img", "input",
            "link", "meta", "param", "source", "track", "wbr"
    );

    /**
     * 修复 Vue 单文件组件中的非法结束标签（如 </img>, </br>）
     * 支持跨行、大小写不敏感、属性含 > 等复杂场景
     *
     * @param projectRootAbsolutePath 项目根目录绝对路径
     * @param relativeFilePath        相对路径（必须是项目内子路径）
     * @return true 如果文件被成功修改
     */
    public static boolean fixInvalidEndTags(String projectRootAbsolutePath, String relativeFilePath) {
        Assert.hasText(projectRootAbsolutePath, "projectRootAbsolutePath must not be blank");
        Assert.hasText(relativeFilePath, "relativeFilePath must not be blank");

        // 防路径穿越
        if (relativeFilePath.contains("..") || relativeFilePath.startsWith("/")) {
            log.warn("拒绝修复非法路径: {}", relativeFilePath);
            return false;
        }

        Path baseDir = Paths.get(projectRootAbsolutePath).normalize();
        Path targetPath = baseDir.resolve(relativeFilePath).normalize();

        // 再次确认目标路径在项目目录内
        if (!targetPath.startsWith(baseDir)) {
            log.warn("拒绝越权路径修复: {}", targetPath);
            return false;
        }

        if (!Files.exists(targetPath)) {
            log.debug("文件不存在，跳过修复: {}", targetPath);
            return false;
        }

        try {
            String content = Files.readString(targetPath, StandardCharsets.UTF_8);
            String originalContent = content;

            // Step 1: 移除所有孤立的 </tag>（无论是否跨行）
            for (String tag : VOID_TAGS) {
                // (?i) = 忽略大小写；\s* = 允许空格；全局替换
                content = content.replaceAll("(?i)</" + Pattern.quote(tag) + "\\s*>", "");
            }

            // Step 2: 将未自闭合的 <tag ...> 转为 <tag ... />
            // 注意：只处理那些不是以 /> 结尾的标签
            for (String tag : VOID_TAGS) {
                // 匹配：<tag ...> 但后面不是 />
                // 使用负向先行断言确保不匹配已自闭合的
                String regex = "(?i)<(" + Pattern.quote(tag) + ")([^>]*)>(?!\\s*/>)";
                content = content.replaceAll(regex, "<$1$2 />");
            }

            if (!content.equals(originalContent)) {
                // 安全写回：先写临时文件，再原子替换
                Path tempFile = Files.createTempFile(targetPath.getParent(), "tmp_", ".vue");
                try {
                    Files.writeString(tempFile, content, StandardCharsets.UTF_8);
                    Files.move(tempFile, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    log.info("✅ 成功修复非法结束标签: {}", relativeFilePath);
                    return true;
                } catch (IOException e) {
                    // 清理临时文件
                    try {
                        Files.deleteIfExists(tempFile);
                    } catch (IOException ignored) {
                        // ignore
                    }
                    throw e;
                }
            } else {
                log.debug("文件无需修复（内容未变化）: {}", relativeFilePath);
                return false;
            }

        } catch (IOException e) {
            log.error("I/O 错误修复文件: {}", relativeFilePath, e);
            return false;
        } catch (Exception e) {
            log.error("修复文件时发生异常: {}", relativeFilePath, e);
            return false;
        }
    }
}
