package com.zriyo.common.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 删除请求类
 *
 * @author Zriyo AI
 * @since 2025-02-17
 */
@Data
public class DeleteRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 单个 ID 删除
     */
    @NotNull(message = "ID 不能为空")
    private Long id;

    /**
     * 批量删除 ID 列表
     */
    private List<Long> ids;
}
