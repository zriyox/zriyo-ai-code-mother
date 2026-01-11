package com.zriyo.user.infrastructure.persistence.repository.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.zriyo.user.domain.model.User;
import com.zriyo.user.domain.repository.UserRepository;
import com.zriyo.user.infrastructure.persistence.converter.UserConverter;
import com.zriyo.user.infrastructure.persistence.mapper.UserMapper;
import com.zriyo.user.infrastructure.persistence.po.UserPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 仓储实现类。
 * 它是基础设施层的一部分，负责实现领域层的接口。
 * 这里我们使用 MyBatis-Flex + MySQL 来实现持久化。
 */
@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserMapper userMapper;
    private final UserConverter userConverter;

    @Override
    public Optional<User> findById(Long id) {
        UserPO po = userMapper.selectOneById(id);
        return Optional.ofNullable(userConverter.toDomain(po));
    }

    @Override
    public Optional<User> findByAccount(String account) {
        UserPO po = userMapper.selectOneByQuery(QueryWrapper.create().eq("userAccount", account));
        return Optional.ofNullable(userConverter.toDomain(po));
    }

    @Override
    public User save(User user) {
        UserPO po = userConverter.toPO(user);
        if (po.getId() == null) {
            userMapper.insert(po);
            user.setId(po.getId()); // 回填 ID 到领域对象
        } else {
            userMapper.update(po);
        }
        return user;
    }

    @Override
    public int update(User user) {
        UserPO po = userConverter.toPO(user);
        int update = userMapper.update(po);
        return update;
    }
}
