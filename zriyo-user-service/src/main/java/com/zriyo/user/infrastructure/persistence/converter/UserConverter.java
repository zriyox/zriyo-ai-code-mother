package com.zriyo.user.infrastructure.persistence.converter;

import com.zriyo.user.domain.model.User;
import com.zriyo.user.infrastructure.persistence.po.UserPO;
import org.springframework.stereotype.Component;

/**
 * 用户对象转换器。
 * 负责 Domain Entity <-> Infrastructure PO 的互转。
 */
@Component
public class UserConverter {

    public User toDomain(UserPO po) {
        if (po == null) {
            return null;
        }
        return User.builder()
                .id(po.getId())
                .userAccount(po.getUserAccount())
                .userName(po.getUserName())
                .userPassword(po.getUserPassword())
                .userAvatar(po.getUserAvatar())
                .userProfile(po.getUserProfile())
                .userRole(po.getUserRole())
                .phone(po.getPhone())
                .authingSub(po.getAuthingSub())
                .createTime(po.getCreateTime())
                .updateTime(po.getUpdateTime())
                .editTime(po.getEditTime())
                .build();
    }

    public UserPO toPO(User user) {
        if (user == null) {
            return null;
        }
        return UserPO.builder()
                .id(user.getId())
                .userAccount(user.getUserAccount())
                .userName(user.getUserName())
                .userPassword(user.getUserPassword())
                .userAvatar(user.getUserAvatar())
                .userProfile(user.getUserProfile())
                .userRole(user.getUserRole())
                .phone(user.getPhone())
                .authingSub(user.getAuthingSub())
                .createTime(user.getCreateTime())
                .updateTime(user.getUpdateTime())
                .editTime(user.getEditTime())
                .isDelete(0) // 默认未删除
                .build();
    }
}
