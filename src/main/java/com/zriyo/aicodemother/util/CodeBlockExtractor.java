package com.zriyo.aicodemother.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CodeBlockExtractor {

    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile(
            "```([\\w\\-]*)\\s*([\\s\\S]*?)```"
    );

    /**
     * 增量处理器，边写边替换代码块占位符
     */
    public static class IncrementalProcessor {
        private final long appId;
        private final String codeGenType;
        private final StringBuilder buffer = new StringBuilder();  // 未处理 chunk 缓存

        public IncrementalProcessor(long appId, String codeGenType) {
            this.appId = appId;
            this.codeGenType = codeGenType;
        }

        /**
         * 追加 chunk 并返回已匹配完整代码块生成的占位符
         */
        public String appendChunk(String chunk) {
            buffer.append(chunk);

            String text = buffer.toString();
            Matcher matcher = CODE_BLOCK_PATTERN.matcher(text);
            StringBuilder output = new StringBuilder();
            int lastIndex = 0;

            while (matcher.find()) {
                output.append(text, lastIndex, matcher.start());

                String language = matcher.group(1).trim();
                String code = matcher.group(2).trim();

                String sanitizedLang = sanitizeLanguage(language);
                String filename = suggestFilename(sanitizedLang, code, codeGenType);
                String fileKey = codeGenType + "_" + appId;

                output.append("[/").append(fileKey).append("/").append(filename).append("]");

                lastIndex = matcher.end();
            }

            // 剩余未闭合部分保留在 buffer
            buffer.setLength(0);
            buffer.append(text.substring(lastIndex));

            return output.toString();
        }

        /**
         * 流结束时调用，处理剩余未闭合部分
         */
        public String finish() {
            String remaining = buffer.toString();
            buffer.setLength(0);

            if (!remaining.isEmpty()) {
                return replaceCodeBlocks(remaining, appId, codeGenType);
            }
            return "";
        }
    }

    /**
     * 原来的完整替换方法
     */
    public static String replaceCodeBlocks(String markdownText, Long appId, String codeGenType) {
        StringBuffer replacedText = new StringBuffer();
        Matcher matcher = CODE_BLOCK_PATTERN.matcher(markdownText);
        int lastIndex = 0;

        while (matcher.find()) {
            replacedText.append(markdownText, lastIndex, matcher.start());

            String language = matcher.group(1).trim();
            String code = matcher.group(2).stripTrailing();

            String sanitizedLang = sanitizeLanguage(language);
            String filename = suggestFilename(sanitizedLang, code, codeGenType);

            String fileKey = codeGenType + "_" + appId;
            replacedText.append("[/").append(fileKey).append("/").append(filename).append("]");

            lastIndex = matcher.end();
        }

        replacedText.append(markdownText.substring(lastIndex));
        return replacedText.toString();
    }

    // ---------------- 语言与文件名逻辑 ----------------
    private static String sanitizeLanguage(String lang) {
        if (lang == null || lang.isEmpty()) return "text";
        lang = lang.toLowerCase().trim();
        switch (lang) {
            case "js": return "javascript";
            case "ts": return "typescript";
            case "html": case "htm": return "html";
            case "css": return "css";
            case "vue": return "vue";
            case "scss": case "sass": return "scss";
            case "less": return "less";
            case "json": return "json";
            case "yaml": case "yml": return "yaml";
            case "toml": return "toml";
            case "md": case "markdown": return "markdown";
            case "sh": case "bash": return "shell";
            case "dockerfile": return "dockerfile";
            case "env": return "env";
            default: return lang;
        }
    }

    private static String suggestFilename(String language, String code, String codeGenType) {
        if ("vue_project".equalsIgnoreCase(codeGenType)) {
            return suggestVueProjectFilename(language, code);
        }
        switch (language) {
            case "javascript": return "main.js";
            case "typescript": return "main.ts";
            case "html": return com.zriyo.aicodemother.model.AppConstant.STATIC_ENTRY_FILE;
            case "css": return "style.css";
            case "vue": return "App.vue";
            case "java": return "Main.java";
            case "python": return "main.py";
            case "json": return "data.json";
            case "xml": return "config.xml";
            case "yaml": return "config.yml";
            case "sql": return "script.sql";
            case "markdown": return "README.md";
            case "scss": return "style.scss";
            case "shell": return "script.sh";
            case "dockerfile": return "Dockerfile";
            case "env": return ".env";
            default:
                if (code.contains("<template>") && code.contains("</template>")) return "Component.vue";
                return "code." + (language.equals("text") ? "txt" : language);
        }
    }

    private static String suggestVueProjectFilename(String language, String code) {
        code = code.toLowerCase();
        if (language.equals("json")) {
            if (code.contains("vite") || code.contains("plugins")) return "vite.config.json";
            if (code.contains("dependencies") && code.contains("devdependencies")) return "package.json";
            if (code.contains("eslint")) return ".eslintrc.json";
            return "tsconfig.json";
        }
        if (language.equals("typescript") || language.equals("javascript")) {
            if (code.contains("createapp") || code.contains("mount")) {
                if (code.contains("main")) return "src/main.ts";
                else return "src/main.js";
            }
            if (code.contains("defineconfig") && (code.contains("vite") || code.contains("build"))) return "vite.config.ts";
            if (code.contains("router")) {
                if (code.contains("createrouter")) return "src/router/index.ts";
                else return "src/router/index.js";
            }
            if (code.contains("pinia") || code.contains("createstore")) return "src/stores/index.ts";
            if (code.contains("export default") && code.contains("setup")) return "src/App.vue";
        }
        if (language.equals("vue")) {
            if (code.contains("<template>") && code.contains("app.vue") || code.contains("<router-view")) return "src/App.vue";
            if (code.contains("home") || code.contains("index")) return "src/views/HomeView.vue";
            if (code.contains("about")) return "src/views/AboutView.vue";
            if (code.contains("layout") || code.contains("navbar")) return "src/components/TheLayout.vue";
            if (code.contains("button") || code.contains("card") || code.contains("component")) return "src/components/BaseButton.vue";
            return "src/components/CustomComponent.vue";
        }
        if (language.equals("css") || language.equals("scss") || language.equals("less")) {
            if (code.contains("body") || code.contains(":root")) return "src/assets/main.css";
            return "src/assets/style.css";
        }
        if (language.equals("env")) {
            if (code.contains("vite")) return ".env";
            return ".env.local";
        }
        if (language.equals("html")) return com.zriyo.aicodemother.model.AppConstant.STATIC_ENTRY_FILE;
        if (language.equals("markdown")) return "README.md";
        if (language.equals("shell") || language.equals("dockerfile")) return language.equals("dockerfile") ? "Dockerfile" : "build.sh";
        return "src/misc/code." + (language.equals("text") ? "txt" : language);
    }

    public static void main(String[] args) {
        System.out.println("===== 测试闭合块 =====");
        String closedInput = """
        ```vue
        <template>
          <div>App</div>
        </template>
        ```
        ```ts
        console.log('hello');
        ```
        """;

        IncrementalProcessor processor1 = new IncrementalProcessor(123L, "vue_project");
        System.out.println("第一次 chunk:\n" + processor1.appendChunk(closedInput));
        System.out.println("结束处理:\n" + processor1.finish());

        System.out.println("\n===== 测试长不闭合块 =====");
        String openInput1 = """
        ```ts
        import { createApp } from 'vue'
        import App from './App.vue'
        const app = createApp(App)
        app.mount('#app')
        console.log('chunk1 end...')
        """;

        String openInput2 = """
        // chunk2 content
        console.log('still open...')
        function test() {
            return 123;
        }
        """;

        String openInput3 = """
        // chunk3 content, finally close
        console.log('last part');
        ```
        """;

        IncrementalProcessor processor2 = new IncrementalProcessor(123L, "vue_project");
        System.out.println("第一次 chunk:\n" + processor2.appendChunk(openInput1));
        System.out.println("第二次 chunk:\n" + processor2.appendChunk(openInput2));
        System.out.println("第三次 chunk:\n" + processor2.appendChunk(openInput3));
        System.out.println("结束处理:\n" + processor2.finish());
    }
}
