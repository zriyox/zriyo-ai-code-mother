package com.zriyo.aicodemother.controller.Admin;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.zriyo.aicodemother.common.BaseResponse;
import com.zriyo.aicodemother.common.DeleteRequest;
import com.zriyo.aicodemother.common.ResultUtils;
import com.zriyo.aicodemother.constant.UserConstant;
import com.zriyo.aicodemother.exception.BusinessException;
import com.zriyo.aicodemother.exception.ErrorCode;
import com.zriyo.aicodemother.exception.ThrowUtils;
import com.zriyo.aicodemother.model.dto.UserAddRequest;
import com.zriyo.aicodemother.model.dto.UserQueryRequest;
import com.zriyo.aicodemother.model.entity.User;
import com.zriyo.aicodemother.model.entity.UserPoints;
import com.zriyo.aicodemother.model.vo.UserAdminVO;
import com.zriyo.aicodemother.service.AppService;
import com.zriyo.aicodemother.service.PointsAccountService;
import com.zriyo.aicodemother.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 后台用户控制层 (重构版：修复了 listMaps 调用和 orderBy 语法)
 */
@RestController
@RequestMapping("/admin/user")
@SaCheckRole(UserConstant.ADMIN_ROLE)
public class AdminUserController {

    @Resource
    private UserService userService;

    @Resource
    private PointsAccountService pointsAccountService;

    @Resource
    private AppService appService;


    /**
     * 分页获取用户列表（仅管理员，包含统计信息，已优化性能）
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<UserAdminVO>> listUserAdminVOByPage(@RequestBody UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = userQueryRequest.getPageNum();
        long pageSize = userQueryRequest.getPageSize();

        // 1. 分页查询用户基础信息
        QueryWrapper queryWrapper = userService.getQueryWrapper(userQueryRequest);
        Page<User> userPage = userService.page(Page.of(pageNum, pageSize), queryWrapper);
        List<User> userList = userPage.getRecords();

        if (CollUtil.isEmpty(userList)) {
            return ResultUtils.success(new Page<>(Collections.emptyList(), pageNum, pageSize, 0));
        }

        // 2. 搜集用户 ID 集合进行批量查询
        Set<Long> userIds = userList.stream().map(User::getId).collect(Collectors.toSet());

        // 3. 批量查询积分信息
        Map<Long, Integer> pointsMap = pointsAccountService.list(QueryWrapper.create()
                .in("user_id", userIds))
                .stream()
                .collect(Collectors.toMap(UserPoints::getUserId, UserPoints::getAvailablePoints, (v1, v2) -> v1));

        // 4. 批量查询应用数量 (修正：使用 AppService 中新增的 countAppByUserIds 方法)
        Map<Long, Long> finalAppCountMap = appService.countAppByUserIds(userIds.stream().toList());

        // 5. 组装 VO
        List<UserAdminVO> userAdminVOList = userList.stream().map(user -> {
            UserAdminVO userAdminVO = new UserAdminVO();
            BeanUtil.copyProperties(user, userAdminVO);
            userAdminVO.setPoints(pointsMap.getOrDefault(user.getId(), 0));
            userAdminVO.setAppCount(finalAppCountMap.getOrDefault(user.getId(), 0L));
            return userAdminVO;
        }).collect(Collectors.toList());
        // 5. 组装结果
        Page<UserAdminVO> userAdminVOPage = new Page<>(userAdminVOList, pageNum, pageSize, userPage.getTotalRow());
        return ResultUtils.success(userAdminVOPage);
    }


    /**
     * 删除用户
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = userService.removeById(deleteRequest.getId());
        return ResultUtils.success(b);
    }

    /**
     * 根据 id 获取用户（仅管理员）
     */
    @GetMapping("/get")
    public BaseResponse<UserAdminVO> getUserById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);

        UserAdminVO userAdminVO = new UserAdminVO();
        BeanUtil.copyProperties(user, userAdminVO);

        // 单个查询积分
        UserPoints userPoints = pointsAccountService.getById(id);
        userAdminVO.setPoints(userPoints != null ? userPoints.getAvailablePoints() : 0);

        // 单个查询应用数量
        long appCount = appService.count(QueryWrapper.create()
                .eq("userId", id));
        userAdminVO.setAppCount(appCount);

        return ResultUtils.success(userAdminVO);
    }


    /**
     * 创建用户
     */
    @PostMapping("/add")
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        ThrowUtils.throwIf(userAddRequest == null, ErrorCode.PARAMS_ERROR);
        User user = new User();
        BeanUtil.copyProperties(userAddRequest, user);
        // 默认密码
        String encryptPassword = userService.getEncryptPassword(UserConstant.DEFAULT_PASSWORD);
        user.setUserPassword(encryptPassword);

        // 设置初始值
        if (user.getUserRole() == null) {
            user.setUserRole(UserConstant.DEFAULT_ROLE);
        }

        boolean result = userService.save(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(user.getId());
    }
}
