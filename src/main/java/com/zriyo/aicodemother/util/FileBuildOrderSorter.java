package com.zriyo.aicodemother.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class FileBuildOrderSorter {

    /**
     * 对文件列表进行排序，确保依赖项先生成
     * 策略：Bottom-Up (被依赖的文件排在前面)
     */
    public static List<String> sort(List<String> files) {
        if (files == null || files.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> sortedList = new ArrayList<>(files);

        // 核心排序逻辑：优先级 + 字母顺序
        sortedList.sort(Comparator
                .comparingInt(FileBuildOrderSorter::getPriorityWeight) // 1. 按权重排序 (小号在前)
                .thenComparing(String::compareTo));                    // 2. 权重相同时，按文件名 A-Z 排序

        return sortedList;
    }

    /**
     * 获取文件权重 (越小越先生成)
     * ----------------------------------------------------
     * Level 0:  配置/工具/样式 (无依赖) -> vite.config.js, .css, utils
     * Level 10: 状态管理/API (被全局依赖) -> store, api
     * Level 20: 基础 UI 组件 (被页面依赖) -> components
     * Level 30: 布局/特殊视图 -> layouts
     * Level 40: 业务页面 (依赖组件/Store) -> pages, views
     * Level 50: 路由定义 (依赖页面) -> router
     * Level 60: 根组件 (依赖路由/Store) -> App.vue
     * Level 70: 入口文件 (依赖所有) -> main.js
     * Level 80: 静态入口 -> index.html
     * Level 99: 其他未知文件
     */
    private static int getPriorityWeight(String filePath) {
        String path = filePath.replace("\\", "/").toLowerCase();

        // 1. 配置文件 & 样式 & 工具类 (最基础)
        if (path.endsWith("vite.config.js") || path.endsWith("vite.config.ts")) return 0;
        if (path.contains("config/")) return 1;
        if (path.endsWith(".css") || path.endsWith(".scss") || path.endsWith(".less") || path.contains("styles/")) return 5;
        if (path.contains("utils/") || path.contains("hooks/") || path.contains("composables/")) return 6;

        // 2. 数据层 (Store / API) - 很多组件和页面会用到
        if (path.contains("store/") || path.contains("stores/") || path.contains("pinia")) return 10;
        if (path.contains("api/") || path.contains("service/")) return 11;

        // 3. UI 组件层 (Components)
        if (path.contains("components/")) return 20;

        // 4. 页面层 (Pages / Views) - 依赖组件和Store
        if (path.contains("pages/") || path.contains("views/")) return 40;

        // 5. 路由层 (Router) - 必须等 Page 生成完，否则没法 import
        if (path.contains("router/") || path.endsWith("router.js") || path.endsWith("router.ts")) return 50;

        // 6. 根组件 (App.vue)
        if (path.endsWith("app.vue")) return 60;

        // 7. JS/TS 入口 (Main)
        if (path.endsWith("main.js") || path.endsWith("main.ts")) return 70;

        // 8. HTML 入口
        if (path.endsWith(com.zriyo.aicodemother.model.AppConstant.STATIC_ENTRY_FILE)) return 80;

        // 兜底
        return 99;
    }

    // --- 测试 Main 方法 ---
    public static void main(String[] args) {
        List<String> unsorted = List.of(
            "src/pages/WritePage.vue",
            "src/main.js",
            "src/components/Envelope.vue",
            "index.html",
            "src/styles/global.css",
            "src/router/index.js",
            "src/store/index.js",
            "src/App.vue",
            "src/components/AppHeader.vue",
            "src/pages/HomePage.vue"
        );

        System.out.println("--- 排序前 ---");
        unsorted.forEach(System.out::println);

        List<String> sorted = FileBuildOrderSorter.sort(unsorted);

        System.out.println("\n--- 排序后 (依赖优先) ---");
        sorted.forEach(System.out::println);
    }
}
