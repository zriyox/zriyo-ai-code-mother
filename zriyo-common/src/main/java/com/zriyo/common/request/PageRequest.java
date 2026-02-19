package com.zriyo.common.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 分页请求基类
 *
 * @author Zriyo AI
 * @since 2025-02-17
 */
@Data
public class PageRequest {

    /**
     * 当前页号（从 1 开始）
     */
    @Min(value = 1, message = "页码最小值为 1")
    private int pageNum = 1;

    /**
     * 页面大小
     */
    @Min(value = 1, message = "每页条数最小值为 1")
    @Max(value = 500, message = "每页条数最大值为 500")
    private int pageSize = 10;

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序顺序（ascend/descend）
     */
    private String sortOrder = "descend";

    /**
     * 获取 MyBatis-Plus 分页偏移量
     */
    public long getOffset() {
        return (long) (pageNum - 1) * pageSize;
    }
}
