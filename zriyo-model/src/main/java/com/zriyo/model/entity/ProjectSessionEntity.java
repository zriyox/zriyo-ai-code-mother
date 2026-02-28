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
 * 项目会话实体
 *
 * @author Zriyo AI
 * @since 2026-02-24
 */
@Data
@EqualsAndHashCode
@TableName("project_session")
public class ProjectSessionEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("app_id")
    private Long appId;

    @TableField("title")
    private String title;

    @TableField("description")
    private String description;

    @TableField("conversation_summary")
    private String conversationSummary;

    @TableField("project_structure")
    private String projectStructure;

    @TableField("active_plan_id")
    private String activePlanId;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
