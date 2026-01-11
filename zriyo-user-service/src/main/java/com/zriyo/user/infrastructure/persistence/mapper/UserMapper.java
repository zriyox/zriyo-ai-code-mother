package com.zriyo.user.infrastructure.persistence.mapper;

import com.mybatisflex.core.BaseMapper;
import com.zriyo.user.infrastructure.persistence.po.UserPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户表 Mapper 接口。
 * 继承 BaseMapper，获得 MyBatis-Flex 提供的通用 CRUD 能力。
 * 泛型指定为 Infrastructure 层的 UserPO。
 */
@Mapper
public interface UserMapper extends BaseMapper<UserPO> {
}
