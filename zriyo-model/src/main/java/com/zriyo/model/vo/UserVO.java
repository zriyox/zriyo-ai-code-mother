package com.zriyo.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户信息返回
 *
 * @author Zriyo AI
 * @since 2025-02-17
 */
@Data
public class UserVO {

    /**
     * 用户 ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

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
     * 状态
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * Token (登录时返回)
     */
    private String token;
}
