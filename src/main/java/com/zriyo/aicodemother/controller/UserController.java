package com.zriyo.aicodemother.controller;

import cn.authing.sdk.java.dto.GeneQRCodeDataDto;
import cn.dev33.satoken.annotation.SaIgnore;
import cn.hutool.core.bean.BeanUtil;
import com.anji.captcha.model.common.ResponseModel;
import com.anji.captcha.model.vo.CaptchaVO;
import com.anji.captcha.service.CaptchaService;
import com.zriyo.aicodemother.common.BaseResponse;
import com.zriyo.aicodemother.common.ResultUtils;
import com.zriyo.aicodemother.exception.ErrorCode;
import com.zriyo.aicodemother.exception.ThrowUtils;
import com.zriyo.aicodemother.model.BindEmailRequest;
import com.zriyo.aicodemother.model.dto.UserLoginRequest;
import com.zriyo.aicodemother.model.dto.UserRegisterRequest;
import com.zriyo.aicodemother.model.dto.UserUpdateRequest;
import com.zriyo.aicodemother.model.entity.User;
import com.zriyo.aicodemother.model.vo.LoginUserVO;
import com.zriyo.aicodemother.model.vo.QrCodeSession;
import com.zriyo.aicodemother.service.AuthingQrCodeService;
import com.zriyo.aicodemother.service.UserService;
import com.zriyo.aicodemother.util.UserAuthUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 前台用户层。
 *
 * @author zriyo
 */
@RestController
@RequestMapping("/user/auto")
@Validated
@Slf4j
public class UserController {

    @Resource
    private UserService userService;
    @Autowired
    private AuthingQrCodeService authingQrCodeService;
    @Autowired
    private CaptchaService captchaService;

    /**
     * 用户注册
     *
     * @param userRegisterRequest 用户注册请求
     * @return 注册结果
     */
    @PostMapping("/register")
    public BaseResponse<LoginUserVO> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        LoginUserVO result = userService.userRegister(userAccount, userPassword, checkPassword, userRegisterRequest.getEmailCode(),userRegisterRequest.getUserName());
        return ResultUtils.success(result);
    }


    /**
     * 用户登录
     *
     * @param userLoginRequest 用户登录请求
     * @param request          请求对象
     * @return 脱敏后的用户登录信息
     */
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
        CaptchaVO captchaVO = new CaptchaVO();
        captchaVO.setCaptchaVerification(userLoginRequest.getCaptchaVerification());
        ResponseModel verification = captchaService.verification(captchaVO);
        ThrowUtils.throwIf(!verification.isSuccess(), ErrorCode.OPERATION_ERROR);
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        LoginUserVO loginUserVO = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(loginUserVO);
    }

    /**
     * 获取当前登录用户
     *
     * @param request 请求对象
     * @return 当前登录用户
     */
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(userService.getLoginUserVO(loginUser));
    }

    /**
     * 用户注销
     *
     * @param request 请求对象
     * @return
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        boolean result = userService.userLogout(request);
        return ResultUtils.success(result);
    }


    /**
     * 更新用户
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        Long userId = UserAuthUtil.getLoginId();
        User user = new User();
        user.setId(userId);
        BeanUtil.copyProperties(userUpdateRequest, user);
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }


    /**
     * 获取微信小程序二维码
     */

    @GetMapping("/qrcode")
    @SaIgnore
    public BaseResponse<GeneQRCodeDataDto> getQrCode() throws Exception {
        GeneQRCodeDataDto geneQRCodeDataDto = authingQrCodeService.generateWechatMiniProgramQrCode();
        return ResultUtils.success(geneQRCodeDataDto);
    }

    /**
     * 获取微信小程序二维码状态
     */
    @GetMapping("/qrcode/status/{qrcodeId}")
    @SaIgnore
    public BaseResponse<QrCodeSession> getQrCodeStatus(@PathVariable String qrcodeId) throws Exception {
        QrCodeSession qrCodeSession = authingQrCodeService.checkQrCodeStatus(qrcodeId);
        return ResultUtils.success(qrCodeSession);
    }

    /**
     * 绑定邮箱
     */
    @PostMapping("/bind/email")
    public BaseResponse<Boolean> bindEmail(@RequestBody BindEmailRequest bindEmailRequest) {
        return ResultUtils.success(userService.bindEmail(bindEmailRequest));
    }


}
