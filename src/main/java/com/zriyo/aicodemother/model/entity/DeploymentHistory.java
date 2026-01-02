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
 * 应用部署历史记录 实体类。
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("deployment_history")
public class DeploymentHistory implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 应用ID
     */
    private Long appId;

    /**
     * 发布名称
     */
    private String deployName;

    /**
     * 唯一部署标识
     */
    private  String version;

    /**
     * 部署时间
     */
    private LocalDateTime deployTime;

}
