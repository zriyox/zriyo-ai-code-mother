package com.zriyo.aicodemother.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.anji.captcha.util.StringUtils;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.util.UpdateEntity;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.zriyo.aicodemother.exception.BusinessException;
import com.zriyo.aicodemother.exception.ErrorCode;
import com.zriyo.aicodemother.mapper.UserMapper;
import com.zriyo.aicodemother.model.BindEmailRequest;
import com.zriyo.aicodemother.model.dto.UserQueryRequest;
import com.zriyo.aicodemother.model.entity.User;
import com.zriyo.aicodemother.model.enums.EmailCaptchaType;
import com.zriyo.aicodemother.model.enums.UserRoleEnum;
import com.zriyo.aicodemother.model.vo.LoginUserVO;
import com.zriyo.aicodemother.model.vo.UserVO;
import com.zriyo.aicodemother.service.UserPointsService;
import com.zriyo.aicodemother.service.UserService;
import com.zriyo.aicodemother.service.email.EmailService;
import com.zriyo.aicodemother.util.BeanCopyUtil;
import com.zriyo.aicodemother.util.UserAuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {


    private final EmailService emailService;

    private final UserPointsService userPointsService;
    private final UserMapper userMapper;

    private String generateUniqueUserName() {
        return "用户_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private void fillBaseUserInfo(User user, String password) {
        if (user.getUserName() == null || user.getUserName().isEmpty()) {
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

    @Override
    public LoginUserVO userRegister(String userAccount, String userPassword, String checkPassword, String emailCode, String userName) {
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
        boolean checkEmailCode = emailService.checkEmailCode(userAccount, emailCode, EmailCaptchaType.REGISTER);
        if (!checkEmailCode) {
            throw new BusinessException(ErrorCode.CHECK_CODE_ERROR);
        }

        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("userAccount", userAccount);
        long count = this.mapper.selectCountByQuery(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
        }

        User user = new User();
        user.setUserName(userName);
        user.setUserAccount(userAccount);
        user.setUserAvatar("https://io.zriyo.com/zriyo-user/default.png");
        fillBaseUserInfo(user, userPassword);

        boolean saveResult = this.save(user);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
        }


        LoginUserVO loginUserVO = toLoginUserVO(user);
        UserAuthUtil.userLogin(user.getId(), loginUserVO);
        loginUserVO.setToken(UserAuthUtil.getTokenValue());
        return loginUserVO;
    }

    @Override
    public LoginUserVO getLoginUserVO(User user) {
        return toLoginUserVO(user);
    }

    private LoginUserVO toLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO vo = new LoginUserVO();
        vo.setId(user.getId());
        vo.setUserAccount(user.getUserAccount());
        vo.setUserName(user.getUserName());
        vo.setUserAvatar(user.getUserAvatar());
        vo.setUserProfile(user.getUserProfile());
        vo.setUserRole(user.getUserRole());
        vo.setCreateTime(user.getCreateTime());
        vo.setUpdateTime(user.getUpdateTime());
        vo.setToken(UserAuthUtil.getTokenValue());
        return vo;
    }

    @Override
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
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = this.mapper.selectOneByQuery(queryWrapper);
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }

        LoginUserVO loginUserVO = toLoginUserVO(user);
        UserAuthUtil.userLogin(user.getId(), loginUserVO);
        String tokenValue = UserAuthUtil.getTokenValue();
        loginUserVO.setToken(tokenValue);
        log.info("用户登入成功：{}", UserAuthUtil.getLoginUserVO());
        return loginUserVO;
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        Long loginId = UserAuthUtil.getLoginId();
        if (loginId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        User userBy = this.getById(loginId);
        if (userBy == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return userBy;
    }

    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream()
                .map(this::getUserVO)
                .collect(Collectors.toList());
    }

    @Override
    public boolean userLogout(HttpServletRequest request) {
        Long loginId = UserAuthUtil.getLoginId();
        if (loginId == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "用户未登录");
        }
        UserAuthUtil.logout();
        return true;
    }

    @Override
    public QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String userAccount = userQueryRequest.getUserAccount();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();

        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("id", id, id != null)
                .eq("userRole", userRole, StrUtil.isNotBlank(userRole))
                .like("userAccount", userAccount, StrUtil.isNotBlank(userAccount))
                .like("userName", userName, StrUtil.isNotBlank(userName))
                .like("userProfile", userProfile, StrUtil.isNotBlank(userProfile));

        if (StrUtil.isNotBlank(sortField)) {
            queryWrapper.orderBy(sortField, "ascend".equals(sortOrder));
        }
        return queryWrapper;
    }

    @Override
    public String getEncryptPassword(String userPassword) {
        final String SALT = "zriyo";
        return DigestUtils.md5DigestAsHex((userPassword + SALT).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public LoginUserVO findOrCreateLocalUser(String authingId, String phone, String photo, String userName) {
        User user = userMapper.selectOneByQuery(new QueryWrapper().eq(User::getAuthingSub, authingId));
        if (Objects.isNull(user)) {
            String uuid = UUID.randomUUID().toString().replace("-", "");
            String password = uuid.substring(0, 16);
            user = new User();
            user.setAuthingSub(authingId);
            user.setPhone(phone);
            user.setUserAvatar(photo);
            if (StrUtil.isNotBlank(userName)) {
                user.setUserName(userName);
            }
            fillBaseUserInfo(user, password);
            userMapper.insert(user);
        }
        return BeanCopyUtil.copy(user, LoginUserVO.class);
    }

    @Override
    public Boolean bindEmail(BindEmailRequest bindEmailRequest) {
        Long userId = UserAuthUtil.getLoginId();
        boolean save = emailService.checkEmailCode(bindEmailRequest.getEmail(), bindEmailRequest.getCode(), EmailCaptchaType.BIND_EMAIL);
        if (!save){
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "邮箱验证码错误");
        }
        User user = userMapper.selectOneByQuery(new QueryWrapper().eq(User::getId, userId));
        if (StringUtils.isNotBlank(user.getUserAccount())){
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "当前已绑定邮箱,无需绑定");
        }
        User user1 = userMapper.selectOneByQuery(new QueryWrapper().eq(User::getUserAccount, bindEmailRequest.getEmail()));
        if (user1 != null){
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "当前邮箱已绑定其他账户");
        }
        User updateUser = UpdateEntity.of(User.class, userId);
        updateUser.setUserAccount(bindEmailRequest.getEmail());
        userMapper.update(updateUser);
        return true;
    }
}
