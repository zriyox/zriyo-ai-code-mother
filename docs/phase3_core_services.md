# 阶段三：全服务 DDD 拆分详解指南

## 1. 目标
本指南为每个微服务提供独立的“施工图纸”。请按照以下顺序一个个服务地进行拆分。确保每个服务都严格遵循 DDD 四层架构。

---

## 2. 公共基础模块 (zriyo-common)

**定位**：存放所有服务共用的工具类、基础实体、异常类。

### 2.1 POM 配置 (`zriyo-common/pom.xml`)
```xml
<artifactId>zriyo-common</artifactId>
<dependencies>
    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
    </dependency>
    <!-- Hutool -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
    </dependency>
    <!-- JSON -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
    </dependency>
</dependencies>
```

### 2.2 代码迁移
- 将 `util` 包下的所有文件移动到 `com.zriyo.common.util`。
- 将 `model.vo.Result` 等通用对象移动到 `com.zriyo.common.model.vo`。
- 将 `exception.GlobalException` 移动到 `com.zriyo.common.exception`。

---

## 3. 用户服务 (zriyo-user-service)

**定位**：负责用户注册、登录、积分、鉴权。

### 3.1 POM 配置 (`zriyo-user-service/pom.xml`)
```xml
<artifactId>zriyo-user-service</artifactId>
<dependencies>
    <!-- 内部依赖 -->
    <dependency>
        <groupId>com.zriyo</groupId>
        <artifactId>zriyo-common</artifactId>
    </dependency>
    <dependency>
        <groupId>com.zriyo</groupId>
        <artifactId>zriyo-api</artifactId>
    </dependency>

    <!-- Nacos & Dubbo -->
    <dependency>
        <groupId>com.alibaba.cloud</groupId>
        <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
    </dependency>
    <dependency>
        <groupId>com.alibaba.cloud</groupId>
        <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
    </dependency>
    <dependency>
        <groupId>org.apache.dubbo</groupId>
        <artifactId>dubbo-spring-boot-starter</artifactId>
    </dependency>

    <!-- 数据库 & Redis -->
    <dependency>
        <groupId>com.mybatis-flex</groupId>
        <artifactId>mybatis-flex-spring-boot-starter</artifactId>
    </dependency>
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
    </dependency>
    <dependency>
        <groupId>org.redisson</groupId>
        <artifactId>redisson-spring-boot-starter</artifactId>
    </dependency>
    
    <!-- Sa-Token -->
    <dependency>
        <groupId>cn.dev33</groupId>
        <artifactId>sa-token-spring-boot3-starter</artifactId>
    </dependency>
</dependencies>
```

### 3.2 目录结构 & 迁移内容
```text
com.zriyo.user
├── UserApplication.java         <-- 新建启动类
├── interfaces
│   └── web                      <-- UserController
├── application
│   └── service                  <-- UserAppService (原 UserServiceImpl 的业务编排部分)
├── domain
│   ├── model                    <-- User (纯净实体，去注解)
│   └── repository               <-- UserRepository (接口)
└── infrastructure
    └── persistence
        ├── mapper               <-- UserMapper (MyBatis)
        ├── po                   <-- UserPO (带数据库注解)
        └── repository           <-- UserRepositoryImpl (实现类)
```

---

## 4. AI 服务 (zriyo-ai-service)

**定位**：负责 LangChain4j 调用、Prompt 管理。

### 4.1 POM 配置 (`zriyo-ai-service/pom.xml`)
```xml
<artifactId>zriyo-ai-service</artifactId>
<dependencies>
    <!-- 基础依赖同上 (Common, API, Nacos, Dubbo) -->
    
    <!-- AI 核心 -->
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j-open-ai-spring-boot-starter</artifactId>
    </dependency>
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j-reactor</artifactId>
    </dependency>
    
    <!-- 响应式 Web (注意：AI服务使用 WebFlux 性能更好) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>
</dependencies>
```

### 4.2 目录结构
```text
com.zriyo.ai
├── AiApplication.java
├── interfaces
│   └── rpc                      <-- AiServiceDubboImpl (对外提供 Dubbo 接口)
├── domain
│   ├── service                  <-- SkeletonGenerateDomainService (核心生成逻辑)
│   └── model                    <-- AiMessage (AI 消息实体)
└── infrastructure
    └── llm                      <-- 具体的 LangChain4j 调用代码
```

---

## 5. 代码生成服务 (zriyo-codegen-service)

**定位**：核心业务编排，调用 AI 服务生成代码，调用 RocketMQ 触发部署。

### 5.1 POM 配置 (`zriyo-codegen-service/pom.xml`)
```xml
<artifactId>zriyo-codegen-service</artifactId>
<dependencies>
    <!-- 基础依赖同上 -->
    
    <!-- RocketMQ (用于触发部署) -->
    <dependency>
        <groupId>org.apache.rocketmq</groupId>
        <artifactId>rocketmq-spring-boot-starter</artifactId>
    </dependency>
</dependencies>
```

### 5.2 核心重构：责任链模式
原有的责任链代码 (`core.pipeline`) 需要迁移到 `domain/service/pipeline`。

- **关键改动**：Handler 中不再直接注入 `UserService` 或 `AiService` 的本地 Bean，而是注入 **Dubbo 所有的远程接口**。
- **示例**：
  ```java
  @Component
  public class ContentGenerateHandler extends AbstractHandler {
      @DubboReference // 改为 Dubbo 调用
      private AiService aiService; 
      
      // ...
  }
  ```

---

## 6. 应用服务 (zriyo-app-service)

**定位**：应用 CRUD、版本管理。

### 6.1 POM 配置 (`zriyo-app-service/pom.xml`)
与 `User Service` 类似，需要 MyBatis-Flex 和 MySQL 驱动。

### 6.2 迁移内容
- **实体**：`App`, `DeploymentHistory` -> `domain/model`
- **逻辑**：`AppService` -> `application/service/AppAppService`

---

## 7. 聊天服务 (zriyo-chat-service)

**定位**：聊天记录存储、日志查询。

### 7.1 POM 配置 (`zriyo-chat-service/pom.xml`)
需要数据库依赖。建议添加 RocketMQ 依赖，用于**异步**接收其他服务发来的日志消息。

```xml
<dependency>
    <groupId>org.apache.rocketmq</groupId>
    <artifactId>rocketmq-spring-boot-starter</artifactId>
</dependency>
```

### 7.2 异步日志落地
在 `interfaces/mq` 包下创建一个 RocketMQ 监听器：
```java
@Component
@RocketMQMessageListener(topic = "CHAT_LOG_TOPIC", consumerGroup = "chat-group")
public class ChatLogListener implements RocketMQListener<ChatLogDTO> {
    @Override
    public void onMessage(ChatLogDTO msg) {
        // 调用 Application Service 保存日志
        chatAppService.saveLog(msg);
    }
}
```

---

## 8. 部署服务 (zriyo-deploy-service)

**定位**：Playwright 自动化截图、S3 上传。

### 8.1 POM 配置 (`zriyo-deploy-service/pom.xml`)
```xml
<dependencies>
    <!-- Playwright -->
    <dependency>
        <groupId>com.microsoft.playwright</groupId>
        <artifactId>playwright</artifactId>
    </dependency>
    <!-- AWS S3 -->
    <dependency>
        <groupId>software.amazon.awssdk</groupId>
        <artifactId>s3</artifactId>
    </dependency>
    <!-- RocketMQ (监听部署指令) -->
    <dependency>
        <groupId>org.apache.rocketmq</groupId>
        <artifactId>rocketmq-spring-boot-starter</artifactId>
    </dependency>
</dependencies>
```

---

**文档创建时间**: 2026-01-01
**当前状态**: 待执行
**说明**: 请务必按照每个服务的 POM 配置添加依赖，这是代码能够编译通过的基础。
