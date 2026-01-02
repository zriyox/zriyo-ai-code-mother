# 阶段一：基础设施与数据库拆分指南

## 1. 目标
完成微服务拆分所需的基础设施搭建，包括：
- ✅ 初始化 Nacos、RocketMQ、MySQL 等中间件
- ✅ 创建拆分后的三个业务数据库 (`user_db`, `app_db`, `chat_db`)
- ✅ 验证中间件的连通性

## 2. 数据库拆分操作

我们已经基于您的现有 schema 生成了三个 SQL 脚本，请按以下步骤执行：

### 步骤 2.1：连接数据库
使用 Navicat 或命令行连接到您的 MySQL 服务器（假设地址为 `192.168.1.10:3306`）。

### 步骤 2.2：创建数据库
执行以下 SQL 语句创建三个新库：

```sql
CREATE DATABASE IF NOT EXISTS zriyo_user_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS zriyo_app_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS zriyo_chat_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 步骤 2.3：导入表结构
分别在对应数据库中运行我们在 `sql/split/` 目录下生成的脚本：

- **zriyo_user_db** -> 执行 `sql/split/zriyo_user_db.sql`
- **zriyo_app_db** -> 执行 `sql/split/zriyo_app_db.sql`
- **zriyo_chat_db** -> 执行 `sql/split/zriyo_chat_db.sql`

## 3. 中间件环境搭建 (Docker Compose)

如果您还未启动中间件，请在 **Server-1 (8核8G)** 上使用我们在部署计划中提供的 `docker-compose-server1.yml` 启动。

```bash
# 1. 进入项目目录
cd /opt/zriyo

# 2. 启动基础服务（不含业务服务）
docker-compose -f docker-compose-server1.yml up -d nacos mysql redis rocketmq-namesrv
```

## 4. RocketMQ 配置

为了支持微服务间的异步解耦和事务消息，需要预先创建 Topic。

### 步骤 4.1：进入 RocketMQ 容器
```bash
docker exec -it rocketmq-namesrv bash
```

### 步骤 4.2：创建 Topic
执行以下命令创建 Topic：

```bash
# 进入工具目录
cd /home/rocketmq/rocketmq-${ROCKETMQ_VERSION}/bin

# 1. 代码生成任务 Topic (事务消息)
sh mqadmin updateTopic -n localhost:9876 -c DefaultCluster -t CODEGEN_TOPIC

# 2. 部署事件 Topic
sh mqadmin updateTopic -n localhost:9876 -c DefaultCluster -t DEPLOY_TOPIC

# 3. 聊天日志 Topic
sh mqadmin updateTopic -n localhost:9876 -c DefaultCluster -t CHAT_LOG_TOPIC
```

## 5. Nacos 配置中心初始化

微服务将共用一些基础配置（如 Redis、RocketMQ 地址），我们将这些作为公共配置发布到 Nacos。

### 步骤 5.1：登录 Nacos
- 地址：`http://192.168.1.10:8848/nacos`
- 账号：`nacos`
- 密码：`nacos`

### 步骤 5.2：创建命名空间
- 命名空间ID：`production`
- 命名空间名称：`生产环境`

### 步骤 5.3：发布公共配置
在 `production` 命名空间下，新建配置文件：

*   **Data ID**: `application-common.yml`
*   **Group**: `DEFAULT_GROUP`
*   **配置格式**: `YAML` (注意不要用 Properties)
*   **配置内容**：

```yaml
spring:
  # Redis 共享配置
  data:
    redis:
      host: redis
      port: 6379
      database: 0
      # password: ${redis_password} # 如果有密码请取消注释

rocketmq:
  # RocketMQ NameServer
  name-server: 192.168.1.10:9876
  producer:
    group: ${spring.application.name}-group

dubbo:
  # Dubbo 注册到 Nacos
  registry:
    address: nacos://nacos:8848
  # 协议配置
  protocol:
    name: dubbo
    port: -1 # 自动分配端口
  consumer:
    check: false # 启动时不检查依赖服务是否可用
```

## 6. 验证清单

完成以上步骤后，请检查：
- [ ] Navicat 能连接三个新数据库，且表结构正确。
- [ ] 浏览器能访问 Nacos 控制台，且能看到 `application-common.yml`。
- [ ] 能够通过 RocketMQ Dashboard (如果有安装) 或命令查看 Topic 是否创建成功。

---
**文档创建时间**: 2026-01-01
**当前状态**: 待执行
