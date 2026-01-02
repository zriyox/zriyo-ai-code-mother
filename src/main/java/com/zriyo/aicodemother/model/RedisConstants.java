package com.zriyo.aicodemother.model;

/**
 * Redis Key 常量类
 * 命名规范：使用冒号分层，如 prefix:sub:key
 */
public final class RedisConstants {

    private RedisConstants() {
        // 私有构造，防止实例化
    }

    /** 判断任务是否正在执行（存在即表示正在运行） */
    public static final String AI_CODE_GEN_TASK_RUNNING = "ai:code:task:running:";




}
