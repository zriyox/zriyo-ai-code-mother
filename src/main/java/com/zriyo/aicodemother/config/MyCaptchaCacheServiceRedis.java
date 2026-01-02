package com.zriyo.aicodemother.config;

import com.anji.captcha.service.CaptchaCacheService;
import com.zriyo.aicodemother.util.RedisUtils;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 自定义 Redis 缓存实现，用于 AJ-Captcha（基于 Redisson + RedisUtils）
 */
@Component
public class MyCaptchaCacheServiceRedis implements CaptchaCacheService {

    @Override
    public void set(String key, String value, long expiresInSeconds) {
        RedisUtils.setCacheObject(key, value, Duration.ofSeconds(expiresInSeconds));
    }

    @Override
    public boolean exists(String key) {
        return RedisUtils.isExistsObject(key);
    }

    @Override
    public void delete(String key) {
        RedisUtils.deleteObject(key);
    }

    @Override
    public String get(String key) {
        return RedisUtils.getCacheObject(key);
    }

    @Override
    public String type() {
        return "redis";
    }

    @Override
    public Long increment(String key, long val) {
        // RedisUtils 没有直接提供 increment，需通过 RedissonClient 手动操作
        return RedisUtils.getClient().getAtomicLong(key).addAndGet(val);
    }

    @Override
    public void setExpire(String key, long seconds) {
        RedisUtils.expire(key, Duration.ofSeconds(seconds));
    }
}
