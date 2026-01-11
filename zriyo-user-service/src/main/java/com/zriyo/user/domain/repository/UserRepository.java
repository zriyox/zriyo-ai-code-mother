package com.zriyo.user.domain.repository;

import com.zriyo.user.domain.model.User;

import java.util.Optional;

/**
 * 领域仓储接口。
 * 只定义业务需要的查、存、删操作，不关心底层数据库。
 */
public interface UserRepository {

    Optional<User> findById(Long id);

    Optional<User> findByAccount(String account);

    User save(User user);

    int update(User user);
}
