# 全栈 DDD 微服务拆分终极手册

**核心目标**：提供一份无需思考、直接执行的操作指南。涵盖目录结构、POM 配置、代码迁移。

---

## 🏗️ 0. 父工程 (Root)

### 0.1 目录结构
```text
zriyo-microservices/
├── pom.xml
├── .gitignore
└── ... (各个子模块文件夹)
```
### 0.2 `pom.xml` (父工程)
只需复制以下 `<modules>` 部分到您的父 POM 中。完整的 `dependencyManagement` 请参考之前 Phase 2 生成的 `pom.xml`。

```xml
<modules>
    <module>zriyo-common</module>
    <module>zriyo-api</module>
    <module>zriyo-gateway</module>
    <module>zriyo-user-service</module>
    <module>zriyo-ai-service</module>
    <module>zriyo-codegen-service</module>
    <module>zriyo-app-service</module>
    <module>zriyo-chat-service</module>
    <module>zriyo-deploy-service</module>
</modules>
```

---

## 🛠️ 1. 公共模块 (zriyo-common)

### 1.1 最终目录结构
```text
zriyo-common
├── pom.xml
└── src/main/java/com/zriyo/common
    ├── constant      <-- 存放常量 (原 constant.*)
    ├── exception     <-- 存放异常 (原 exception.*)
    ├── model
    │   └── vo        <-- 存放 Result.java
    └── util          <-- 存放所有工具类 (原 util.*)
```

### 1.2 `pom.xml`
```xml
<project>
    <parent>
        <groupId>com.zriyo</groupId>
        <artifactId>zriyo-ai-code-mother</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>
    <artifactId>zriyo-common</artifactId>

    <dependencies>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
        </dependency>
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>
        <!-- 基础 Web 注解依赖 -->
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-web</artifactId>
        </dependency>
    </dependencies>
</project>
```

---

## 👤 2. 用户服务 (zriyo-user-service)

### 2.1 最终目录结构
```text
zriyo-user-service
├── pom.xml
└── src/main/java/com/zriyo/user
    ├── UserApplication.java         <-- [NEW] 启动类
    ├── config                       <-- [MOVE] SaTokenConfigure
    ├── interfaces                   <-- [接口层]
    │   └── web
    │       └── UserController.java  <-- [MOVE] 原 Controller
    ├── application                  <-- [应用层]
    │   └── service
    │       └── UserAppService.java  <-- [MOVE & RENAME] 原 ServiceImpl
    ├── domain                       <-- [领域层]
    │   ├── model
    │   │   └── User.java            <-- [MOVE & CLEAN] 纯净实体
    │   └── repository
    │       └── UserRepository.java  <-- [NEW] 接口
    └── infrastructure               <-- [基础设施层]
        └── persistence
            ├── mapper
            │   └── UserMapper.java  <-- [MOVE] 原 Mapper
            ├── po
            │   └── UserPO.java      <-- [COPY] 带数据库注解的实体
            └── repository
                └── UserRepositoryImpl.java <-- [NEW] 实现类
```

### 2.2 `pom.xml`
```xml
<project>
    <parent>
        <groupId>com.zriyo</groupId>
        <artifactId>zriyo-ai-code-mother</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>
    <artifactId>zriyo-user-service</artifactId>

    <dependencies>
        <dependency>
            <groupId>com.zriyo</groupId>
            <artifactId>zriyo-common</artifactId>
        </dependency>
        <dependency>
            <groupId>com.zriyo</groupId>
            <artifactId>zriyo-api</artifactId>
        </dependency>
        <!-- Nacos, Dubbo, MySQL, MyBatis-Flex, Redisson, Sa-Token -->
        <!-- (请确保父POM中已导入相应 Starter，此处省略重复声明以节省篇幅，参照 Phase 3 文档) -->
         <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
        </dependency>
         <dependency>
            <groupId>org.apache.dubbo</groupId>
            <artifactId>dubbo-spring-boot-starter</artifactId>
        </dependency>
         <dependency>
            <groupId>com.mybatis-flex</groupId>
            <artifactId>mybatis-flex-spring-boot-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
        </dependency>
    </dependencies>
</project>
```

---

## 🤖 3. AI 服务 (zriyo-ai-service)

### 3.1 最终目录结构
```text
zriyo-ai-service
├── pom.xml
└── src/main/java/com/zriyo/ai
    ├── AiApplication.java
    ├── interfaces
    │   └── rpc
    │       └── AiServiceDubboImpl.java <-- [NEW] 对外暴露 Dubbo 接口
    ├── domain
    │   ├── model
    │   │   └── AiContext.java          <-- [MOVE] AI 上下文对象
    │   └── service
    │       └── AiDomainService.java    <-- [MOVE] 核心 LangChain 调用逻辑
    └── infrastructure
        └── llm
             └── LangChainFactory.java  <-- [NEW] 封装 LangChain4j 工厂
```

### 3.2 `pom.xml`
```xml
<project>
    <dependencies>
        <dependency>
            <groupId>com.zriyo</groupId>
            <artifactId>zriyo-common</artifactId>
        </dependency>
        <dependency>
            <groupId>dev.langchain4j</groupId>
            <artifactId>langchain4j-open-ai-spring-boot-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webflux</artifactId>
        </dependency>
        <!-- Dubbo, Nacos -->
    </dependencies>
</project>
```

---

## ⚡ 4. 代码生成服务 (zriyo-codegen-service)

### 4.1 最终目录结构
```text
zriyo-codegen-service
├── pom.xml
└── src/main/java/com/zriyo/codegen
    ├── CodegenApplication.java
    ├── interfaces
    │   └── web
    │       └── GenerateController.java  <-- [MOVE] 接收前端生成请求
    ├── application
    │   └── service
    │       └── GenAppService.java       <-- [MOVE] 协调生成流程
    ├── domain
    │   ├── pipeline                     <-- [MOVE] 原 core.pipeline.*
    │   │   ├── Context.java
    │   │   ├── Handler.java
    │   │   └── impl                     <-- 各种具体 Handler
    │   └── model
    │       └── GenTask.java             <-- 任务聚合根
    └── infrastructure
        └── remote
            ├── RemoteAiService.java     <-- [NEW] 封装 Dubbo 调用 AI 服务
            └── RemoteUserService.java   <-- [NEW] 封装 Dubbo 调用用户服务
```

### 4.2 `pom.xml`
所有核心依赖 + RocketMQ：
```xml
<dependency>
    <groupId>org.apache.rocketmq</groupId>
    <artifactId>rocketmq-spring-boot-starter</artifactId>
</dependency>
```

---

**文档创建时间**: 2026-01-01
**说明**: 本文档作为最终执行标准，替代之前的 Phase 2 & 3 文档。
