package com.zriyo.aicodemother.mapper;

import com.mybatisflex.core.BaseMapper;
import com.zriyo.aicodemother.model.entity.App;
import com.zriyo.aicodemother.model.vo.AppPageVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 应用 映射层。
 *
 * @author zriyo
 */
public interface AppMapper extends BaseMapper<App> {

    // 1. 查询应用列表（带 user 信息）
    List<AppPageVO> selectAppWithUserList(
            @Param("priority") Integer priority,
            @Param("appName") String appName,
            @Param("offset") long offset,
            @Param("limit") long limit
    );

    // 2. 查询总数量（用于分页）
    long countAppWithUser(
            @Param("priority") Integer priority,
            @Param("appName") String appName
    );

    // 3. 批量统计每个用户的应用数
    List<Map<String, Object>> countAppByUserIds(@Param("userIds") List<Long> userIds);

}
