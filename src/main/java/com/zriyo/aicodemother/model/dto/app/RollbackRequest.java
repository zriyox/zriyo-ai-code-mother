package com.zriyo.aicodemother.model.dto.app;

import lombok.Data;

import java.io.Serializable;

@Data
public class RollbackRequest implements Serializable {
    // 回滚id
    private Long rollbackId;
    // 应用id
    private Long appId;
    private static final long serialVersionUID = 1L;
}
