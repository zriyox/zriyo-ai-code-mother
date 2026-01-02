package com.zriyo.aicodemother.ai.tools;

import com.zriyo.aicodemother.model.AppConstant;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public class CodeReadTool {
    private final Long appId;

    public CodeReadTool(Long appId) {
        this.appId = appId;
    }


    @Tool("读取项目中的文件内容。当需要查看某个文件的源码实现时，请使用此工具。" +
            "注意：只能读取当前 Vue 项目的文件，路径必须是相对于项目根目录的相对路径，例如 'src/App.vue'。")
    public String readFile(@P("需要读取的文件相对路径，例如 'src/main.js'") String relativePath) {
        try {
            if (relativePath == null || relativePath.trim().isEmpty()) {
                return "Error: 文件路径不能为空";
            }

            relativePath = relativePath.trim();
            if (relativePath.startsWith("/") || relativePath.startsWith("\\")) {
                return "Error: 路径不能以 '/' 或 '\\' 开头，请使用相对路径";
            }

            String baseDir = AppConstant.VUE_PROJECT_PREFIX + this.appId;
            Path base = Paths.get(baseDir).toAbsolutePath().normalize();

            Path targetPath = base.resolve(relativePath).normalize();
            log.info("readFile 尝试访问文件: appId={}, path={}", this.appId, relativePath);
            if (!targetPath.startsWith(base)) {
                return "Error: 访问被拒绝，路径超出项目范围: " + relativePath;
            }

            if (!Files.exists(targetPath)) {
                return "Error: 文件不存在: " + relativePath;
            }

            if (!Files.isReadable(targetPath)) {
                return "Error: 文件不可读: " + relativePath;
            }

            String content = Files.readString(targetPath, StandardCharsets.UTF_8);
            log.info("readFile 工具调用成功: appId={}, path={}", this.appId, relativePath);
            return content;

        } catch (Exception e) {
            log.warn("readFile 工具调用失败: appId={}, path={}, error={}", this.appId, relativePath, e.getMessage());
            return "Error: 读取文件失败 - " + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

}
