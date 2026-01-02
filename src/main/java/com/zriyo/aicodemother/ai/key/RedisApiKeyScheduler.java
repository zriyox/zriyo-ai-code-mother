package com.zriyo.aicodemother.ai.key;

import com.zriyo.aicodemother.model.enums.AiCodeGenStage;
import com.zriyo.aicodemother.util.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMapCache;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RedisApiKeyScheduler {

    private static final int MAX_PER_KEY = 3;
    private static final int FAILURE_THRESHOLD = 5;
    private static final String FAIL_COUNT_PREFIX = "ai:key:fail:count:";
    private static final String BAN_LABEL_PREFIX = "ai:key:ban:";
    private static final String SOFT_BAN_PREFIX = "ai:key:softban:";
    private static final String LEASE_CACHE_PREFIX = "ai:key:active_leases:";
    private static final int SOFT_BAN_DURATION_SEC = 60;
    private static final int LEASE_TTL_MIN = 10;

    private final ConcurrentHashMap<AiCodeGenStage, String> stageModelMap = new ConcurrentHashMap<>();

    private RedissonClient client() {
        return RedisUtils.getClient();
    }

    public void setStageModelMap(AiCodeGenStage stage, String modelName) {
        this.stageModelMap.put(stage, modelName);
    }

    public void registerModelKeys(AiCodeGenStage stage, Collection<String> apiKeys) {
        String modelName = getModelName(stage);
        String redisKey = poolKey(modelName);
        RScoredSortedSet<String> zset = client().getScoredSortedSet(redisKey);

        for (String key : apiKeys) {
            if (!zset.contains(key)) {
                zset.add(MAX_PER_KEY, key);
            }
        }
        log.info("[AI-KeyPool] {} 注册完成，Model: {}, 数量: {}", stage, modelName, apiKeys.size());
    }

    public String acquire(AiCodeGenStage stage) {
        String modelName = getModelName(stage);
        String redisKey = poolKey(modelName);
        RScoredSortedSet<String> zset = client().getScoredSortedSet(redisKey);

        return RedisUtils.executeWithLockAndReturn(redisKey, 200, () -> {
            String lastResortKey = null;

            Collection<String> allKeys = zset.valueRangeReversed(0, -1);
            if (allKeys.isEmpty()) {
                throw new IllegalStateException("当前阶段 " + stage + " 模型池中没有任何注册的 API Key");
            }

            for (String key : allKeys) {
                if (client().getBucket(BAN_LABEL_PREFIX + key).isExists()) {
                    continue;
                }

                boolean isSoftBanned = client().getBucket(SOFT_BAN_PREFIX + key).isExists();

                Double score = zset.getScore(key);
                if (score != null && score > 0) {
                    if (!isSoftBanned) {
                        return consumeKey(zset, key, score);
                    } else {
                        if (lastResortKey == null) {
                            lastResortKey = key;
                        }
                    }
                }
            }

            if (lastResortKey != null) {
                log.warn("[AI-KeyPool] {} 处于冷却期，但作为唯一可用 Key 被强制启用", mask(lastResortKey));
                Double score = zset.getScore(lastResortKey);
                return consumeKey(zset, lastResortKey, score);
            }

            throw new RuntimeException("当前阶段 " + stage + " 无可用 API Key，所有 Key 已达并发上限或已被硬封禁");
        });
    }

    private String consumeKey(RScoredSortedSet<String> zset, String key, Double currentScore) {
        zset.add(currentScore - 1, key);
        String leaseId = UUID.randomUUID().toString();
        client().getMapCache(LEASE_CACHE_PREFIX + key)
                .put(leaseId, 1, LEASE_TTL_MIN, TimeUnit.MINUTES);
        return key;
    }

    public void release(AiCodeGenStage stage, String apiKey) {
        String modelName = getModelName(stage);
        RScoredSortedSet<String> zset = client().getScoredSortedSet(poolKey(modelName));

        if (zset.contains(apiKey)) {
            RMapCache<String, Integer> leases = client().getMapCache(LEASE_CACHE_PREFIX + apiKey);
            if (!leases.isEmpty()) {
                leases.fastRemove(leases.keySet().iterator().next());
            }

            Double currentScore = zset.getScore(apiKey);
            if (currentScore != null && currentScore < MAX_PER_KEY) {
                zset.add(currentScore + 1, apiKey);
            }
        }
    }

    /**
     * 【增强版】成功汇报：
     * 1. 清空失败计数
     * 2. 解除软封禁
     * 3. 【新增】将该 Key 在所有已知模型池中的分数重置为 MAX_PER_KEY（即完全返还额度）
     */
    public void reportSuccess(String apiKey) {
        // 1. 清空失败计数
        client().getAtomicLong(FAIL_COUNT_PREFIX + apiKey).delete();
        // 2. 清除软封禁
        client().getBucket(SOFT_BAN_PREFIX + apiKey).delete();

        // 3. 【关键新增】重置该 Key 在所有模型池中的分数为 MAX_PER_KEY
        if (!stageModelMap.isEmpty()) {
            for (String modelName : stageModelMap.values()) {
                String redisKey = poolKey(modelName);
                RScoredSortedSet<String> zset = client().getScoredSortedSet(redisKey);
                if (zset.contains(apiKey)) {
                    // 直接设置分数为 MAX_PER_KEY（覆盖当前值）
                    zset.add(MAX_PER_KEY, apiKey);
                }
            }
        }

        log.debug("[AI-KeyPool] Key {} 请求成功，已重置失败计数并完全返还额度", mask(apiKey));
    }

    public void reportFailure(String apiKey, AiCodeGenStage stage) {
        client().getBucket(SOFT_BAN_PREFIX + apiKey).set("COOLING", SOFT_BAN_DURATION_SEC, TimeUnit.SECONDS);
        log.warn("[AI-KeyPool] Key {} 进入 {} 秒冷却期", mask(apiKey), SOFT_BAN_DURATION_SEC);

        String redisFailKey = FAIL_COUNT_PREFIX + apiKey;
        long count = client().getAtomicLong(redisFailKey).incrementAndGet();
        client().getAtomicLong(redisFailKey).expire(24, TimeUnit.HOURS);

        if (count >= FAILURE_THRESHOLD) {
            log.error("[AI-KeyPool] Key {} 累计失败 {} 次，触发硬封禁", mask(apiKey), count);
            banKeyUntilSixAm(apiKey);
            client().getAtomicLong(redisFailKey).delete();
            client().getBucket(SOFT_BAN_PREFIX + apiKey).delete();
        } else {
            log.warn("[AI-KeyPool] Key {} 故障统计: {}/{}", mask(apiKey), count, FAILURE_THRESHOLD);
        }
    }

    private void banKeyUntilSixAm(String apiKey) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime target = now.with(LocalTime.of(6, 0, 0));
        if (now.isAfter(target)) {
            target = target.plusDays(1);
        }
        long secondsUntilSix = Duration.between(now, target).getSeconds();
        client().getBucket(BAN_LABEL_PREFIX + apiKey).set("BANNED", secondsUntilSix, TimeUnit.SECONDS);
    }

    public void disable(AiCodeGenStage stage, String apiKey) {
        String modelName = getModelName(stage);
        client().getScoredSortedSet(poolKey(modelName)).remove(apiKey);
    }

    private String getModelName(AiCodeGenStage stage) {
        String name = stageModelMap.get(stage);
        if (name == null) throw new IllegalStateException("未找到阶段 " + stage + " 的模型映射");
        return name;
    }

    @Scheduled(fixedDelay = 60000)
    public void watchdogAuditKeys() {
        if (stageModelMap.isEmpty()) return;
        for (String modelName : stageModelMap.values()) {
            String redisKey = poolKey(modelName);
            RScoredSortedSet<String> zset = client().getScoredSortedSet(redisKey);

            for (String apiKey : zset.valueRange(0, -1)) {
                int activeLeaseCount = client().getMapCache(LEASE_CACHE_PREFIX + apiKey).size();
                int targetScore = MAX_PER_KEY - activeLeaseCount;
                Double currentScore = zset.getScore(apiKey);

                if (currentScore != null && currentScore.intValue() != targetScore) {
                    zset.add(targetScore, apiKey);
                }
            }
        }
    }

    private String poolKey(String modelName) { return "ai:keypool:" + modelName; }
    private String mask(String k) { return k.length() > 8 ? k.substring(0, 8) + "****" : "****"; }
}
