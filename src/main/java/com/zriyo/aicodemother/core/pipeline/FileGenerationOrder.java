package com.zriyo.aicodemother.core.pipeline;

import com.zriyo.aicodemother.model.dto.ProjectSkeletonDTO;

import java.util.*;

/**
 * 文件生成顺序排序器 (AI 上下文优化版)
 * 策略：基于目录结构的权重排序 (Bottom-Up)
 * 目的：确保生成上层文件(如 Page)时，底层的依赖(如 Component/Store)已经在磁盘上
 */
public class FileGenerationOrder {

    public static List<String> computeSafeOrder(ProjectSkeletonDTO skeleton) {
        if (skeleton == null || skeleton.getFiles() == null) {
            return Collections.emptyList();
        }

        Map<String, ProjectSkeletonDTO.FileInfo> files = skeleton.getFiles();
        List<String> result = new ArrayList<>();

        // 1. 构建邻接表和入度表
        Map<String, List<String>> adj = new HashMap<>();
        Map<String, Integer> inDegree = new HashMap<>();

        for (String path : files.keySet()) {
            inDegree.putIfAbsent(path, 0);
            ProjectSkeletonDTO.FileInfo info = files.get(path);
            if (info.getLocalDependencies() != null) {
                for (String dep : info.getLocalDependencies()) {
                    // 仅处理项目内存在的依赖
                    if (files.containsKey(dep)) {
                        adj.computeIfAbsent(dep, k -> new ArrayList<>()).add(path);
                        inDegree.put(path, inDegree.getOrDefault(path, 0) + 1);
                    }
                }
            }
        }

        // 2. 使用优先队列进行拓扑排序 (入度为 0 的节点进入队列)
        // 队列排序参考之前的权重比较器，保证同级别下权重基础的文件优先
        PriorityQueue<String> queue = new PriorityQueue<>(new FilePriorityComparator());
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.offer(entry.getKey());
            }
        }

        // 3. 执行排序
        while (!queue.isEmpty()) {
            String curr = queue.poll();
            result.add(curr);

            List<String> neighbors = adj.get(curr);
            if (neighbors != null) {
                for (String neighbor : neighbors) {
                    inDegree.put(neighbor, inDegree.get(neighbor) - 1);
                    if (inDegree.get(neighbor) == 0) {
                        queue.offer(neighbor);
                    }
                }
            }
        }

        // 4. 环路检查与兜底
        if (result.size() != files.size()) {
            System.err.println("警告: 生成顺序检查到循环依赖，执行兜底排序策略");
            // 找出漏掉的文件并按权重排序补全
            List<String> missing = new ArrayList<>(files.keySet());
            missing.removeAll(result);
            missing.sort(new FilePriorityComparator());
            result.addAll(missing);
        }

        return result;
    }

    /**
     * 核心排序逻辑比较器 (作为拓扑排序的二次筛选条件)
     */
    static class FilePriorityComparator implements Comparator<String> {
        @Override
        public int compare(String path1, String path2) {
            int weight1 = getPriorityWeight(path1);
            int weight2 = getPriorityWeight(path2);

            if (weight1 != weight2) {
                return Integer.compare(weight1, weight2);
            }
            if (path1.length() != path2.length()) {
                return Integer.compare(path1.length(), path2.length());
            }
            return path1.compareTo(path2);
        }
    }

    private static int getPriorityWeight(String filePath) {
        if (filePath == null) return 999;
        String path = filePath.replace("\\", "/").toLowerCase();

        // Level 0: 配置
        if (path.contains("config") || path.endsWith(".json") || path.startsWith(".env")) return 0;
        // Level 1: 样式
        if (path.endsWith(".css") || path.endsWith(".scss") || path.contains("styles/")) return 10;
        // Level 2: 超基础工具 (Constants)
        if (path.contains("constants/")) return 15;
        // Level 3: 通用工具 (Utils/Hooks)
        if (path.contains("utils/") || path.contains("hooks/") || path.contains("composables/")) return 20;
        // Level 4: 数据层
        if (path.contains("store/")) return 30;
        if (path.contains("api/") || path.contains("services/")) return 31;
        // Level 5: 组件
        if (path.contains("components/")) return 40;
        // Level 6: 布局
        if (path.contains("layouts/")) return 50;
        // Level 7: 页面
        if (path.contains("pages/") || path.contains("views/")) return 60;
        // Level 8: 路由
        if (path.contains("router/")) return 70;
        // Level 9: 入口
        if (path.endsWith("app.vue")) return 80;
        if (path.endsWith("main.js") || path.endsWith("main.ts")) return 90;
        // Level 10: HTML
        if (path.endsWith("index.html")) return 100;

        return 500;
    }
}
