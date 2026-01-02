# DDD 架构设计与拆分指南

## 1. 什么是 DDD (领域驱动设计)？

DDD (Domain-Driven Design) 是一种以**业务领域**为核心的软件架构设计方法。与传统的“数据驱动”（先建表，再写代码）不同，DDD 强调先理解业务，再建模。

### 1.1 核心概念

#### 🌍 限界上下文 (Bounded Context)
- 定义了模型的**边界**。一个概念（如“商品”）在不同上下文中有不同含义。
- **微服务原则**：一个微服务通常对应一个（或少数几个紧密相关的）限界上下文。
- **ZriyoCode 案例**：`用户上下文`、`AI生成上下文`、`应用管理上下文`。

#### 🏗️ 四层架构 (The Layered Architecture)
DDD 推荐将系统划分为四层，严格控制依赖方向（只能从上往下调用）。

1.  **用户接口层 (User Interface / Interfaces)**
    - **职责**：处理外部请求（HTTP/RPC），解析参数，返回结果。
    - **对应代码**：Controller, DTO。
2.  **应用层 (Application)**
    - **职责**：**用例编排**。它不包含业务逻辑，只负责协调。例如：“开启事务 -> 调用领域对象行为 -> 保存 -> 发送事件”。
    - **对应代码**：ApplicationService (AppService)。
3.  **领域层 (Domain) —— 核心！**
    - **职责**：**业务逻辑**。包含业务规则、状态流转。这一层应该是纯净的 Java 代码，尽量不依赖 Spring 或数据库注解。
    - **对应代码**：Aggregate (聚合根), Entity (实体), Value Object (值对象), DomainService (领域服务), Repository Interface (仓储接口)。
4.  **基础设施层 (Infrastructure)**
    - **职责**：提供技术实现。如数据库访问、Redis 操作、MQ 发送、文件上传。
    - **对应代码**：Mapper, Repository Impl, Util, Config。

#### 🩸 充血模型 vs 贫血模型
- **贫血模型 (Anemic)**：传统的 MVC 模式。`User` 类只有 Getter/Setter，业务逻辑全在 `UserService` 里。
- **充血模型 (Rich)**：`User` 类拥有行为。例如 `user.changePassword(newPwd)`，对象自己管理自己的状态。DDD 提倡充血模型。

---

## 2. ZriyoCode 的 DDD 工程结构

我们将把每个微服务模块（如 `zriyo-user-service`）设计为如下结构：

```text
com.zriyo.user
├── interfaces             <-- [接口层] Web/RPC 入口
│   ├── web                (Controller)
│   ├── rpc                (Dubbo Provider 实现)
│   └── dto                (数据传输对象)
│
├── application            <-- [应用层] 用例编排
│   ├── service            (UserAppService, AuthService)
│   └── event              (应用层事件监听)
│
├── domain                 <-- [领域层] 核心业务
│   ├── model              (领域模型)
│   │   ├── User.java      (聚合根)
│   │   ├── UserId.java    (值对象)
│   │   └── ...
│   ├── service            (UserDomainService - 处理跨实体逻辑)
│   └── repository         (UserRepository - 仓储接口)
│
└── infrastructure         <-- [基础设施层] 技术实现
    ├── persistence        (持久化)
    │   ├── mapper         (MyBatis-Flex Mapper)
    │   ├── po             (Persistent Object - 数据库表映射对象)
    │   └── repository     (UserRepositoryImpl - 仓储实现)
    ├── util               (工具类)
    └── config             (配置类)
```

### 2.1 依赖倒置原则 (DIP)
在 DDD 中，**领域层不应该依赖基础设施层**。
- **错误**：`Domain` 直接调用 `Mapper`。
- **正确**：`Domain` 定义 `Repository Interface`（接口），`Infrastructure` 实现这个接口。运行时通过 Spring 注入实现。

---

## 3. 实战：从 MVC 到 DDD 的迁移演练

以“修改用户昵称”为例：

### 传统 MVC 方式
```java
// UserService.java
public void updateUserName(Long id, String newName) {
    // 1. 业务校验混在 Service 里
    if (newName == null) throw new Exception("...");
    
    // 2. 直接操作数据对象
    UserPO user = userMapper.selectById(id);
    user.setUserName(newName);
    
    // 3. 调用持久层
    userMapper.update(user);
}
```

### DDD 方式

**1. 领域层 (Domain)**
```java
// User.java (聚合根)
public class User {
    private String userName;
    
    // 行为：修改昵称 (业务逻辑内聚)
    public void changeName(String newName) {
        if (newName == null) throw new DomainException("昵称不能为空");
        this.userName = newName;
        // 可以触发领域事件...
    }
}
```

**2. 基础设施层 (Infrastructure)**
```java
// UserRepoImpl.java
@Repository
public class UserRepositoryImpl implements UserRepository {
    @Autowired UserMapper mapper;
    
    public void save(User user) {
        // 将领域对象转为 PO 并保存
        UserPO po = convert(user);
        mapper.update(po);
    }
}
```

**3. 应用层 (Application)**
```java
// UserAppService.java
public void updateUserName(Long id, String newName) {
    // 1. 恢复领域对象
    User user = userRepository.find(id);
    
    // 2. 执行领域行为
    user.changeName(newName);
    
    // 3. 持久化
    userRepository.save(user);
}
```

---

## 4. 总结与下一步

通过 DDD，我们将业务逻辑从 Service 中剥离，回归到对象本身（Domain Model），使代码更具可测试性和可维护性。

**下一步行动**：
我们将基于此 DDD 结构，重新梳理 **阶段二：代码工程拆分指南**，指导您按四层架构创建目录。
