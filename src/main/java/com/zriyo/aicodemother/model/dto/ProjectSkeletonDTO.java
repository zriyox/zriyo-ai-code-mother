package com.zriyo.aicodemother.model.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 项目骨架传输对象
 * 对应 AI 生成的 JSON 结构，包含文件树、依赖关系及逻辑契约。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProjectSkeletonDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 整个项目的文件集合 (Key: filePath, Value: FileInfo) */
    private Map<String, FileInfo> files;

    /** 全局信息（依赖、样式指南等） */
    private GlobalInfo global;

    /*--------------------------------------------------*
     * FileInfo：单个文件的元数据与逻辑定义
     *--------------------------------------------------*/
    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FileInfo implements Serializable {

        /** 文件相对路径 (e.g., "src/components/BentoCard.vue") */
        private String filePath;

        /** 文件类型 (vue, js, css, html) */
        private String type;

        /** 文件描述 */
        private String description;


        /** * [关键新增] 接口逻辑契约
         * 描述 Props 定义、Emits 事件、JS 导出函数签名或 Store State 结构
         * e.g., "Props: { title: String }. Emits: ['click']"
         */
        private String interfaceDef;

        /** 导出列表 */
        private List<String> exports;

        /** 外部 NPM 依赖 (e.g., "vue", "vue-router") */
        private List<String> dependencies;

        /** 本地文件依赖路径列表 (e.g., "src/utils/mockData.js") */
        private List<String> localDependencies;

        /** Import 语句详情列表 */
        private List<ImportInfo> imports;

        /** Vue 模板中使用的组件名列表 (仅 Vue 文件有效) */
        private List<String> templateComponents;

        /**
         * 任意未来扩展字段 (e.g., 作者信息, 修改时间)
         */
        private Map<String, JsonNode> extra = new HashMap<>();

        @JsonAnySetter
        public void captureExtra(String key, JsonNode value) {
            extra.put(key, value);
        }
    }

    /*--------------------------------------------------*
     * ImportInfo：单行 import 描述
     *--------------------------------------------------*/
    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ImportInfo implements Serializable {

        /** 导入的变量名 (副作用导入时为 null) */
        private String importedName;

        /** 来源路径 (支持 @ 别名) */
        private String sourcePath;

        /** 导入类型 (js, vue, css) */
        private String type;

        /** 是否为默认导入 (default import) */
        private Boolean isDefault;
    }

    /*--------------------------------------------------*
     * GlobalInfo：全局配置
     *--------------------------------------------------*/
    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GlobalInfo implements Serializable {

        /** 全局描述 */
        private String description;

        /** 全局依赖版本 (Key: package, Value: version) */
        private Map<String, String> dependencies;

        /**
         * 样式指南 (Style Guide)
         * 包含 colors, typography, layout, conventions 等
         * 由于结构灵活（可能包含 JIT 策略描述），使用 Map<String, Object> 兜底
         */
        private Map<String, Object> styleGuide;

        /** 全局扩展字段 */
        private Map<String, JsonNode> extra = new HashMap<>();

        @JsonAnySetter
        public void captureExtra(String key, JsonNode value) {
            extra.put(key, value);
        }
    }
}
