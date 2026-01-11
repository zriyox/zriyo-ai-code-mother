package com.zriyo.user.infrastructure.persistence.po;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户 持久化对象 (Persistent Object)。
 * 对应数据库表结构，包含所有 MyBatis-Flex 注解。
 *
 * @author zriyo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("user")
public class UserPO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    @Column("userAccount")
    private String userAccount;

    @Column("userPassword")
    private String userPassword;

    @Column("userName")
    private String userName;

    @Column("userAvatar")
    private String userAvatar;

    @Column("userProfile")
    private String userProfile;

    @Column("userRole")
    private String userRole;

    @Column("phone")
    private String phone;

    @Column("authing_sub")
    private String authingSub;

    @Column("editTime")
    private LocalDateTime editTime;

    @Column("createTime")
    private LocalDateTime createTime;

    @Column("updateTime")
    private LocalDateTime updateTime;

    @Column(value = "isDelete", isLogicDelete = true)
    private Integer isDelete;
}
