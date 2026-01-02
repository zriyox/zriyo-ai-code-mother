package com.zriyo.aicodemother.config.satoken;

import cn.dev33.satoken.stp.StpInterface;
import com.zriyo.aicodemother.model.entity.User;
import com.zriyo.aicodemother.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 自定义权限加载接口实现类
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    @Resource
    private UserService userService;

    /**
     * 返回一个账号所拥有的权限码集合
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // TODO 获取精细权限
        List<String> list = new ArrayList<String>();
        list.add("art.*");
        return list;
    }

    /**
     * 返回一个账号所拥有的角色标识集合 (权限与角色可分开校验)
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        String userId = (String) loginId;
        Long userIdLong = Long.parseLong(userId);
        User user = userService.getById(userIdLong);
        if (user != null) {
            return List.of(user.getUserRole());
        }
        return List.of();
    }

}
