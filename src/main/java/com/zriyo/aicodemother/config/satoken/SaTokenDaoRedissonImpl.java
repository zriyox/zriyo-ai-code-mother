package com.zriyo.aicodemother.config.satoken;

import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.session.SaSession;
import org.redisson.api.RBucket;
import org.redisson.api.RKeys;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class SaTokenDaoRedissonImpl implements SaTokenDao {

    @Autowired
    private RedissonClient redissonClient;

    // ==================== String 类型操作 ====================

    @Override
    public String get(String key) {
        RBucket<String> bucket = redissonClient.getBucket(key);
        return bucket.get();
    }

    @Override
    public void set(String key, String value, long timeout) {
        RBucket<String> bucket = redissonClient.getBucket(key);
        if (timeout <= 0) {
            bucket.set(value);
        } else {
            bucket.set(value, timeout, TimeUnit.SECONDS);
        }
    }

    @Override
    public void update(String key, String value) {
        RBucket<String> bucket = redissonClient.getBucket(key);
        if (bucket.isExists()) {
            bucket.set(value);
        }
    }

    @Override
    public void delete(String key) {
        redissonClient.getBucket(key).delete();
    }

    @Override
    public long getTimeout(String key) {
        long ttl = redissonClient.getBucket(key).remainTimeToLive();
        return ttl <= 0 ? -2 : ttl / 1000; // -2 表示不存在或永不过期（按 Sa-Token 规范）
    }

    @Override
    public void updateTimeout(String key, long timeout) {
        if (timeout <= 0) {
            redissonClient.getBucket(key).clearExpire();
        } else {
            redissonClient.getBucket(key).expire(timeout, TimeUnit.SECONDS);
        }
    }

    // ==================== Object / SaSession 操作 ====================

    @Override
    public Object getObject(String key) {
        RBucket<Object> bucket = redissonClient.getBucket(key);
        return bucket.get();
    }

    @Override
    public <T> T getObject(String key, Class<T> clazz) {
        Object obj = getObject(key);
        return clazz.isInstance(obj) ? clazz.cast(obj) : null;
    }

    @Override
    public void setObject(String key, Object object, long timeout) {
        RBucket<Object> bucket = redissonClient.getBucket(key);
        if (timeout <= 0) {
            bucket.set(object);
        } else {
            bucket.set(object, timeout, TimeUnit.SECONDS);
        }
    }

    @Override
    public void updateObject(String key, Object object) {
        RBucket<Object> bucket = redissonClient.getBucket(key);
        if (bucket.isExists()) {
            bucket.set(object);
        }
    }

    @Override
    public void deleteObject(String key) {
        redissonClient.getBucket(key).delete();
    }

    @Override
    public long getObjectTimeout(String key) {
        return getTimeout(key); // 复用逻辑
    }

    @Override
    public void updateObjectTimeout(String key, long timeout) {
        updateTimeout(key, timeout);
    }

    // ==================== SaSession 专用方法（推荐使用） ====================

    @Override
    public SaSession getSession(String sessionId) {
        return getObject(sessionId, SaSession.class);
    }

    @Override
    public void setSession(SaSession session, long timeout) {
        setObject(session.getId(), session, timeout);
    }

    @Override
    public void updateSession(SaSession session) {
        updateObject(session.getId(), session);
    }

    @Override
    public void deleteSession(String sessionId) {
        deleteObject(sessionId);
    }

    @Override
    public long getSessionTimeout(String sessionId) {
        return getObjectTimeout(sessionId);
    }

    @Override
    public void updateSessionTimeout(String sessionId, long timeout) {
        updateObjectTimeout(sessionId, timeout);
    }


    /**
     * 搜索符合 pattern 的 key 列表（用于在线用户、踢人下线等）
     *
     * @param prefix   key 前缀，如 "satoken:login:token:"
     * @param keyword  关键词（Sa-Token 内部传入，但 Redisson 不支持带关键词的 SCAN，故忽略）
     * @param start    起始位置
     * @param size     获取数量
     * @param sortType 是否排序
     * @return 匹配的 key 列表
     */
    @Override
    public List<String> searchData(String prefix, String keyword, int start, int size, boolean sortType) {
        RKeys keys = redissonClient.getKeys();
        Iterable<String> iterable = keys.getKeysByPattern(prefix + "*");

        List<String> matchedKeys = new ArrayList<>();
        for (String key : iterable) {
            // 可选：根据 keyword 进一步过滤（性能敏感时慎用）
            // if (keyword != null && !key.contains(keyword)) continue;
            matchedKeys.add(key);
        }

        if (sortType) {
            matchedKeys.sort(String::compareTo);
        }

        // 分页
        int fromIndex = Math.min(start, matchedKeys.size());
        int toIndex = Math.min(start + size, matchedKeys.size());
        return matchedKeys.subList(fromIndex, toIndex);
    }
}
