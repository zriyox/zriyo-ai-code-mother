package com.zriyo.model.dto;

import com.zriyo.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户实体
 *
 * @author Zriyo AI
 * @since 2025-02-17
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserEntity extends BaseEntity {

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 状态 (0=禁用, 1=启用)
     */
    private Integer status;
}
