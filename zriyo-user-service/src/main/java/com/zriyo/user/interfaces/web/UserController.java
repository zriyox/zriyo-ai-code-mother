package com.zriyo.user.interfaces.web;

import cn.authing.sdk.java.dto.GeneQRCodeDataDto;
import cn.dev33.satoken.annotation.SaIgnore;
import cn.hutool.core.bean.BeanUtil;
import com.anji.captcha.model.common.ResponseModel;
import com.anji.captcha.model.vo.CaptchaVO;
import com.anji.captcha.service.CaptchaService;
import com.zriyo.api.dto.*;
import com.zriyo.api.vo.LoginUserVO;
import com.zriyo.api.vo.QrCodeSession;
import com.zriyo.common.BaseResponse;
import com.zriyo.common.ResultUtils;
import com.zriyo.common.exception.BusinessException;
import com.zriyo.common.exception.ErrorCode;
import com.zriyo.common.exception.ThrowUtils;
import com.zriyo.common.util.UserAuthUtil;
import com.zriyo.user.application.service.UserAppService;
import com.zriyo.user.domain.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 用户接口控制器。
 * 负责接收 HTTP 请求，调用 Application Service，返回统一结果。
 */
@RestController
@RequestMapping("/user/auto")
@Slf4j
@Validated
@RequiredArgsConstructor
public class UserController {

    private final UserAppService userAppService;
    private final CaptchaService captchaService;

    /**
     * 用户注册
     *
     * @param userRegisterRequest 用户注册请求
     * @return 注册结果
     */
    @PostMapping("/register")
    public Object userRegister(@Valid @RequestBody UserRegisterRequest userRegisterRequest) {
        if (userRegisterRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        LoginUserVO result = userAppService.userRegister(userAccount, userPassword, checkPassword);
        return  ResultUtils.success(result);
    }

    /**
     * 用户登录
     *
     * @param userLoginRequest 用户登录请求
     * @param request          请求对象
     * @return 脱敏后的用户登录信息
     */
    @PostMapping("/login")
    public Object userLogin(@Valid @RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        if (userLoginRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        CaptchaVO captchaVO = new CaptchaVO();
        captchaVO.setCaptchaVerification(userLoginRequest.getCaptchaVerification());
        ResponseModel verification = captchaService.verification(captchaVO);
        ThrowUtils.throwIf(!verification.isSuccess(), ErrorCode.OPERATION_ERROR);
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        LoginUserVO loginUserVO = userAppService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(loginUserVO);
    }

    /**
     * 用户注销
     *
     * @param request 请求对象
     * @return 成功返回
     */
    @PostMapping("/logout")
    public Object userLogout(HttpServletRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = userAppService.userLogout(request);
        return ResultUtils.success(result);
    }

    /**
     * 获取当前登录用户
     *
     * @param request 请求对象
     * @return 当前登录用户
     */
    @GetMapping("/get/login")
    public Object getLoginUser(HttpServletRequest request) {
        User user = userAppService.getLoginUser(request);
        return ResultUtils.success(userAppService.getLoginUserVO(user));
    }

    /**
     * 更新用户
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateUser( @RequestBody UserUpdateRequest userUpdateRequest) {
        Long userId = UserAuthUtil.getLoginId();
        User user = new User();
        user.setId(userId);
        BeanUtil.copyProperties(userUpdateRequest, user);
        boolean result = userAppService.updateUser(user);
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
    public BaseResponse<Boolean> bindEmail(@Valid @RequestBody BindEmailRequest bindEmailRequest) {
        return ResultUtils.success(userAppService.bindEmail(bindEmailRequest));
    }

    /**
     * 找回密码
     */
    @PostMapping("/forget/password")
    public BaseResponse<LoginUserVO> forgetPassword(@Valid @RequestBody ForgetPasswordRequest forgetPasswordRequest) {
        return ResultUtils.success(userAppService.forgetPassword(forgetPasswordRequest));
    }
}
