package com.zriyo.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 工作流定义实体
 *
 * @author Zriyo AI
 * @since 2026-02-24
 */
@Data
@EqualsAndHashCode
@TableName("workflow_def")
public class WorkflowDefEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("workflow_id")
    private String workflowId;

    @TableField("version")
    private Integer version;

    @TableField("name")
    private String name;

    @TableField("spec_json")
    private String specJson;

    @TableField("enabled")
    private Integer enabled;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
