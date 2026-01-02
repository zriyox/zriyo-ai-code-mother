package com.zriyo.aicodemother.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiToolLogVo {
    /**
     * 工具调用 Id
     */
    private Long id;
    /**
     * 对应的 AI 消息 ID（哪次对话调用的）
     */
    private Long aiMessageId;

    /**
     * 工具名称（writeFile、createFolder 等）
     */
    private String toolName;

    /**
     * 工具操作的文件路径
     */
    private String filePath;

    /**
     * 工具执行动作（写入/读取/创建）
     */
    private String action;

    /**
     * 对这次操作的简短描述
     */
    private String summary;

    /**
     * 操作耗时 单位秒
     */
    private int costTime;

}
