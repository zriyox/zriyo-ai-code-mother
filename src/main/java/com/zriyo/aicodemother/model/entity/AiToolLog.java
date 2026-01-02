package com.zriyo.aicodemother.model.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.sql.Timestamp;

/**
 * AI 工具调用记录表 实体类。
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("ai_tool_log")
public class AiToolLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @Id(keyType = KeyType.Auto)
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
     * 操作耗时
     */
    private int costTime;

    /**
     * 创建时间
     */
    private Timestamp createdAt;

    /**
     * 更新时间
     */
    private Timestamp updatedAt;

}
