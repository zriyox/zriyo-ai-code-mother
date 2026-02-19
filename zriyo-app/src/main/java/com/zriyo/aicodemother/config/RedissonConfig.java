package com.zriyo.aicodemother.config;

import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 配置类
 */
@Configuration
@Slf4j
public class RedissonConfig {

    @Autowired
    private RedisProperties redisProperties;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        String address = "redis://" + redisProperties.getHost() + ":" + redisProperties.getPort();

        // 获取超时配置，如果为空则使用默认值10秒
        int timeout;
        if (redisProperties.getTimeout() == null) {
            timeout = 10000; // 默认10秒
            log.warn("Redis timeout not configured, using default value: {}ms", timeout);
        } else {
            timeout = (int) redisProperties.getTimeout().toMillis();
        }

        var singleServerConfig = config.useSingleServer()
                .setAddress(address)
                .setDatabase(redisProperties.getDatabase())
                .setTimeout(timeout);

        // 只有在密码不为空时才设置密码
        if (redisProperties.getPassword() != null && !redisProperties.getPassword().isEmpty()) {
            singleServerConfig.setPassword(redisProperties.getPassword());
            log.info("Redisson configured with password for: {}", address);
        } else {
            log.info("Redisson configured without password for: {}", address);
        }

        return Redisson.create(config);
    }
}
