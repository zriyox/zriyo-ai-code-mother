# 阶段二：DDD 代码迁移对照表 (傻瓜式操作指南)

## 1. 简介
本指南专为初次接触 DDD 的开发者设计。您不需要深入理解复杂的理论，只需按照下表，将文件从“原单体项目”搬运到“新微服务模块”的指定位置即可。

## 2. 核心规则
在 DDD 中，我们通常会把原来的一个类拆成两个：
- **Entity (领域实体)**：保留业务字段，去掉数据库注解（`@Table`, `@Id`）。放在 `domain/model`。
- **PO (持久化对象)**：保留数据库注解，专门用于 MyBatis 读写数据库。放在 `infrastructure/persistence/po`。

---

## 3. 文件搬运对照表 (zriyo-user-service)

请参照下表，将原项目 (`backup`) 中的文件移动到 `zriyo-user-service/src/main/java/com/zriyo/user` 下的对应目录。

| 原文件 (原路径) | 新文件位置 (新路径) | 这里的代码要做什么改动？ |
|----------------|-------------------|----------------------|
| **Controller** | | |
| `controller/UserController.java` | `interfaces/web/UserController.java` | 1. 修改包名为 `com.zriyo.user.interfaces.web`<br>2. 将注入的 `UserService` 改为 `UserAppService`<br>3. 返回值保持 `Result` 不变 |
| **Service** | | |
| `service/impl/UserServiceImpl.java` | `application/service/UserAppService.java` | **改名！**<br>1. 类名改为 `UserAppService`<br>2. 去掉 `implements UserService` (不再实现旧接口)<br>3. 将直接调用的 `userMapper` 改为调用 `userRepository` |
| **Entity** | | **这里要拆成两个！** |
| `model/entity/User.java` | 1. `domain/model/User.java` | **去注解！**<br>去掉 `@Table`, `@Id`, `@Column` 等数据库注解，只保留字段和 Getter/Setter。 |
| (同上) | 2. `infrastructure/persistence/po/UserPO.java` | **留注解！**<br>类名改为 `UserPO`，保留所有数据库注解。这是给 Mapper 用的。 |
| **Mapper** | | |
| `mapper/UserMapper.java` | `infrastructure/persistence/mapper/UserMapper.java` | 1. 泛型修改：`BaseMapper<User>` -> `BaseMapper<UserPO>`<br>2. 确保它操作的是 PO 对象 |

---

## 4. 需要新建的文件 (胶水代码)

为了让上面的代码跑起来，您需要**新建**以下几个文件：

### 4.1 仓储接口 (在 Domain 层)
**位置**：`domain/repository/UserRepository.java`
```java
package com.zriyo.user.domain.repository;

import com.zriyo.user.domain.model.User;

public interface UserRepository {
    User findById(Long id);
    void save(User user);
    // 定义其他业务需要的查询方法
}
```

### 4.2 仓储实现 (在 Infrastructure 层)
**位置**：`infrastructure/persistence/repository/UserRepositoryImpl.java`
```java
package com.zriyo.user.infrastructure.persistence.repository;

import org.springframework.stereotype.Repository;
import com.zriyo.user.domain.repository.UserRepository;
import com.zriyo.user.infrastructure.persistence.mapper.UserMapper;
import com.zriyo.user.infrastructure.persistence.po.UserPO;
import com.zriyo.user.domain.model.User;
import cn.hutool.core.bean.BeanUtil;

@Repository
public class UserRepositoryImpl implements UserRepository {
    
    // 注入 Mapper
    private final UserMapper userMapper;
    
    public UserRepositoryImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public void save(User user) {
        // 关键步骤：Domain -> PO
        UserPO po = BeanUtil.copyProperties(user, UserPO.class);
        if (po.getId() == null) {
            userMapper.insert(po);
            user.setId(po.getId()); // 回填ID
        } else {
            userMapper.update(po);
        }
    }
    
    @Override
    public User findById(Long id) {
        UserPO po = userMapper.selectOneById(id);
        // 关键步骤：PO -> Domain
        return BeanUtil.copyProperties(po, User.class);
    }
}
```

---

## 5. 公共模块搬运 (zriyo-common)

这些文件比较简单，直接原样移动，只改包名。

| 原文件路径 | 新位置 (zriyo-common 下) |
|-----------|------------------------|
| `util/*.java` | `src/main/java/com/zriyo/common/util/` |
| `model/vo/Result.java` | `src/main/java/com/zriyo/common/model/vo/` |
| `exception/*.java` | `src/main/java/com/zriyo/common/exception/` |
| `constant/*.java` | `src/main/java/com/zriyo/common/constant/` |

---
**小贴士**：
1. 先搬 `zriyo-common`，因为其他服务都依赖它。
2. 搬运完一个文件的第一件事：**修改 package 声明**。
3. 如果 IDE 报红（找不到类），尝试 `Alt+Enter` (IntelliJ) 重新导入正确的包。
