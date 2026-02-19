package com.zriyo.aicodemother.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置
 *
 * @author Zriyo AI
 * @since 2025-02-17
 */
@Configuration
@MapperScan("com.zriyo.service.mapper")
public class MyBatisPlusConfig {

    // 暂时简化配置，后续可以添加分页、乐观锁等拦截器

}
