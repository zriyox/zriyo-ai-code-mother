package com.zriyo.user.application.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.zriyo.api.enums.UserRoleEnum;
import com.zriyo.api.vo.LoginUserVO;
import com.zriyo.api.vo.UserVO;
import com.zriyo.common.exception.BusinessException;
import com.zriyo.common.exception.ErrorCode;
import com.zriyo.common.util.UserAuthUtil;
import com.zriyo.user.domain.model.User;
import com.zriyo.user.domain.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 用户应用服务 (Application Service)。
 * 负责编排业务流程（注册、登录、信息修改）。
 * 只依赖 Domain Layer (UserRepository, User Entity) 和 Shared API (DTO/VO)。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserAppService {

    private final UserRepository userRepository;

    // 依赖的其他服务（暂时通过 Dubbo/Feign 或模块间调用，这里假设已在 api 模块定义接口或 mock）
    // private final EmailService emailService;
    // private final UserPointsService userPointsService;

    // TODO: 真正的微服务拆分中，EmailService 应该是远程调用。为了演示方便，这里保留调用逻辑注释

    private String generateUniqueUserName() {
        return "用户_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private void fillBaseUserInfo(User user, String password) {
        if (StrUtil.isBlank(user.getUserName())) {
            user.setUserName(generateUniqueUserName());
        }
        if (user.getUserRole() == null) {
            user.setUserRole(UserRoleEnum.USER.getValue());
        }
        if (user.getUserPassword() == null) {
            user.setUserPassword(getEncryptPassword(password));
        }
        LocalDateTime now = LocalDateTime.now();
        user.setCreateTime(now);
        user.setUpdateTime(now);
        user.setEditTime(now);
    }

    @Transactional(rollbackFor = Exception.class)
    public LoginUserVO userRegister(String userAccount, String userPassword, String checkPassword) {
        if (StrUtil.hasBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号长度过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度过短");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }

        // TODO: 校验邮箱验证码逻辑 (调用 EmailService)
        // boolean checkEmailCode = emailService.checkEmailCode(userAccount, emailCode, EmailCaptchaType.REGISTER);

        Optional<User> existingUser = userRepository.findByAccount(userAccount);
        if (existingUser.isPresent()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
        }

        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserAvatar("https://io.zriyo.com/zriyo-user/default.png");
        fillBaseUserInfo(user, userPassword);

        userRepository.save(user);

        LoginUserVO loginUserVO = toLoginUserVO(user);
        UserAuthUtil.userLogin(user.getId(), loginUserVO);
        loginUserVO.setToken(UserAuthUtil.getTokenValue());
        return loginUserVO;
    }

    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        if (StrUtil.hasBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号长度过短");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度过短");
        }

        String encryptPassword = getEncryptPassword(userPassword);
        User user = userRepository.findByAccount(userAccount)
                .filter(u -> u.getUserPassword().equals(encryptPassword))
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误"));

        LoginUserVO loginUserVO = toLoginUserVO(user);
        UserAuthUtil.userLogin(user.getId(), loginUserVO);
        loginUserVO.setToken(UserAuthUtil.getTokenValue());
        log.info("用户登入成功：{}", loginUserVO.getUserName());
        return loginUserVO;
    }

    public User getLoginUser(HttpServletRequest request) {
        Long loginId = UserAuthUtil.getLoginId();
        if (loginId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return userRepository.findById(loginId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_LOGIN_ERROR));
    }

    public boolean userLogout(HttpServletRequest request) {
        if (UserAuthUtil.getLoginId() == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "用户未登录");
        }
        UserAuthUtil.logout();
        return true;
    }

    public LoginUserVO getLoginUserVO(User user) {
        return toLoginUserVO(user);
    }

    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        return userVO;
    }

    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream()
                .map(this::getUserVO)
                .collect(Collectors.toList());
    }

    private LoginUserVO toLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO vo = new LoginUserVO();
        BeanUtil.copyProperties(user, vo);
        vo.setToken(UserAuthUtil.getTokenValue());
        return vo;
    }

    public boolean updateUser(User user){
        int row = userRepository.update(user);
        return row > 0;
    }


    // 密码加密逻辑 (建议放在 Domain Service，这里暂时放在 Application Service)
    private String getEncryptPassword(String userPassword) {
        final String SALT = "zriyo";
        return DigestUtils.md5DigestAsHex((userPassword + SALT).getBytes(StandardCharsets.UTF_8));
    }


}
