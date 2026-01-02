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
import java.time.LocalDateTime;

/**
 * AI代码生成调用记录表 实体类。
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("ai_code_gen_record")
public class AiCodeGenRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 关联的应用ID（即项目ID）
     */
    private Long appId;

    /**
     * 发起用户的ID
     */
    private Long userId;

    /**
     * 状态：RUNNING / SUCCESS / FAILED / CANCELLED
     */
    private String status;

    /**
     * 当前阶段：INIT / SKELETON / FILE_GENERATION / BUILD / DONE
     */
    private String stage;

    /**
     * 失败时的错误信息
     */
    private String errorMessage;

    /**
     * 生成的项目目录名（如 vue_project_123）
     */
    private String projectDir;

    /**
     * 哪句消息发起的调用
     */
    private Long messageId;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间（可为空）
     */
    private LocalDateTime endTime;

    /**
     * 耗时（毫秒）
     */
    private Long durationMs;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
