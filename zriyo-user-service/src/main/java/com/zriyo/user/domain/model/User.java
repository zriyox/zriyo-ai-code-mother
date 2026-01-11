package com.zriyo.user.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户 领域实体类 (Domain Entity)。
 * 只关注业务属性，不包含数据库注解。
 *
 * @author zriyo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** id */
    private Long id;

    /** 账号 */
    private String userAccount;

    /** 密码 */
    private String userPassword;

    /** 用户昵称 */
    private String userName;

    /** 用户头像 */
    private String userAvatar;

    /** 用户简介 */
    private String userProfile;

    /** 用户角色 */
    private String userRole;

    /** 手机号 */
    private String phone;

    /** 唯一标识 */
    private String authingSub;

    /** 编辑时间 */
    private LocalDateTime editTime;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
