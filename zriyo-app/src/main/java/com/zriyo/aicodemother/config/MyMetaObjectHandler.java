package com.zriyo.aicodemother.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 字段自动填充处理器
 *
 * @author Zriyo AI
 * @since 2025-02-17
 */
@Slf4j
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();

        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);

        // TODO: 从 Sa-Token 获取当前用户 ID
        Long currentUserId = getCurrentUserId();
        this.strictInsertFill(metaObject, "createBy", Long.class, currentUserId);
        this.strictInsertFill(metaObject, "updateBy", Long.class, currentUserId);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());

        Long currentUserId = getCurrentUserId();
        this.strictUpdateFill(metaObject, "updateBy", Long.class, currentUserId);
    }

    /**
     * 获取当前用户 ID
     * TODO: 集成 Sa-Token 后从上下文获取
     */
    private Long getCurrentUserId() {
        // 暂时返回系统用户 ID
        return 0L;
    }
}
