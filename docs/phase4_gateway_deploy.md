# 阶段四：网关与部署指南

## 1. 目标
搭建统一的流量入口（Gateway），配置路由与鉴权，并完成最终的容器化部署。

## 2. API 网关配置 (zriyo-gateway)

网关负责将外部 HTTP 请求路由到内部微服务。

### 2.1 POM 配置
文件路径：`zriyo-gateway/pom.xml`

**注意**：网关基于 WebFlux，**切勿引入** `spring-boot-starter-web` (Tomcat)，否则会启动失败。

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <groupId>com.zriyo</groupId>
        <artifactId>zriyo-ai-code-mother</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>

    <artifactId>zriyo-gateway</artifactId>

    <dependencies>
        <dependency>
            <groupId>com.zriyo</groupId>
            <artifactId>zriyo-common</artifactId>
        </dependency>

        <!-- Gateway -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-gateway</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-loadbalancer</artifactId>
        </dependency>

        <!-- Nacos -->
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
        </dependency>
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
        </dependency>

        <!-- Sa-Token 网关鉴权 (Reactive版) -->
        <dependency>
            <groupId>cn.dev33</groupId>
            <artifactId>sa-token-reactor-spring-boot3-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
        </dependency>
    </dependencies>
</project>
```

### 2.2 路由规则配置
在 Nacos 配置中心的 `zriyo-gateway.yml` 中配置：

```yaml
server:
  port: 8080

spring:
  cloud:
    gateway:
      discovery:
        locator:
          enabled: true # 开启通过服务名自动访问
          lower-case-service-id: true
      routes:
        # 用户服务路由
        - id: user-service
          uri: lb://zriyo-user-service
          predicates:
            - Path=/api/user/**
          filters:
            - StripPrefix=1 # 去除 /api 前缀 (视Controller定义而定)

        # 代码生成服务路由
        - id: codegen-service
          uri: lb://zriyo-codegen-service
          predicates:
            - Path=/api/codegen/**

        # App服务路由
        - id: app-service
          uri: lb://zriyo-app-service
          predicates:
            - Path=/api/app/**
            
sa-token:
  # 权限认证配置
  token-name: satoken
  timeout: 2592000
  active-timeout: -1
  is-concurrent: true
  is-share: true
  token-style: uuid
  is-read-body: false
  is-read-header: true
```

### 2.3 全局过滤器 (GlobalFilter)
请在网关模块中实现一个 `CodeLoomGlobalFilter`，用于处理跨域（CORS）和统一鉴权逻辑（如果未使用 Sa-Token 的配置模式）。

---

## 3. 部署上线检查清单

在执行 `docker-compose up` 之前，请务必检查：

### ✅ 基础设施检查
- [ ] Nacos (8848) 是否正常运行？
- [ ] MySQL (3306) 是否已创建 3 个拆分后的数据库？
- [ ] RocketMQ (9876) 是否已创建 `CODEGEN_TOPIC` 等主题？

### ✅ 配置中心检查
- [ ] Nacos 中是否已创建 `application-common.yml`？
- [ ] MySQL 和 Redis 的密码是否正确？
- [ ] 对应服务的 `.yml` 配置是否已发布？

### ✅ 镜像构建检查
- [ ] 父工程 `mvn clean package` 是否成功？
- [ ] 各个子模块的 Docker 镜像是否已打包？
  ```bash
  docker build -t zriyo-gateway:1.0.0 ./zriyo-gateway
  docker build -t zriyo-user-service:1.0.0 ./zriyo-user-service
  ```

---
**文档创建时间**: 2026-01-01
**当前状态**: 待参考
