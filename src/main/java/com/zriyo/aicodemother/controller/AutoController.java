package com.zriyo.aicodemother.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.anji.captcha.model.common.ResponseModel;
import com.anji.captcha.model.vo.CaptchaVO;
import com.anji.captcha.service.CaptchaService;
import com.zriyo.aicodemother.common.BaseResponse;
import com.zriyo.aicodemother.common.ResultUtils;
import com.zriyo.aicodemother.exception.BusinessException;
import com.zriyo.aicodemother.exception.ErrorCode;
import com.zriyo.aicodemother.exception.ThrowUtils;
import com.zriyo.aicodemother.model.dto.*;
import com.zriyo.aicodemother.model.enums.EmailCaptchaType;
import com.zriyo.aicodemother.service.ImageProxyService;
import com.zriyo.aicodemother.service.email.EmailService;
import com.zriyo.aicodemother.util.BeanCopyUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.Duration;

/**
 * 统一校验接口
 */
@RestController
@RequestMapping("/")
@Validated
@RequiredArgsConstructor
@Slf4j
public class AutoController {


    private final ImageProxyService imageProxyService;
    private final CaptchaService captchaService;
    private final EmailService emailService;

    /**
     * 获取邮箱验证码
     */
    @PostMapping("/auto/code")
    public BaseResponse<Boolean> getCode(@Valid @RequestBody EmailCodeRequest request) {
        String email = request.getEmail();
        String captchaVerification = request.getCaptchaVerification();
        CaptchaVO captchaVO = new CaptchaVO();
        captchaVO.setCaptchaVerification(captchaVerification);
        ResponseModel verification = captchaService.verification(captchaVO);
        ThrowUtils.throwIf(!verification.isSuccess(), ErrorCode.CHECK_CAPTCHA_ERROR);
        EmailCaptchaType emailCaptchaType = EmailCaptchaType.fromKey(request.getCaptchaType().getKey());
        ThrowUtils.throwIf(emailCaptchaType == null, ErrorCode.PARAMS_ERROR);
        emailService.getEmailCode(email, emailCaptchaType);
        return ResultUtils.success(true);
    }

    /**
     * 生成行为验证码
     */
    @PostMapping("/auto/captcha")
    public BaseResponse<Object> captcha(@RequestBody CaptchaRequest request) {
        CaptchaVO captchaVO = new CaptchaVO();
        captchaVO.setCaptchaType(request.getCaptchaType().getValue());
        ResponseModel responseModel = captchaService.get(captchaVO);
        Object repData = responseModel.getRepData();
        CaptchaResponseData copy = BeanCopyUtil.copy(repData, CaptchaResponseData.class);
        return ResultUtils.success(copy);
    }

    /**
     * 校验行为验证码
     */
    @PostMapping("/auto/captcha/match")
    public BaseResponse<CaptchaVerifyResultResponse> captchaMatch(@RequestBody CaptchaVerifyRequest data) {
        CaptchaVO captchaVO = new CaptchaVO();
        captchaVO.setCaptchaType(data.getCaptchaType().getValue());
        captchaVO.setToken(data.getToken());
        captchaVO.setPointJson(data.getPointJson());
        ResponseModel check = captchaService.check(captchaVO);
        if (!check.isSuccess()) {
            throw new BusinessException(ErrorCode.CHECK_CAPTCHA_ERROR);
        }
        CaptchaVerifyResultResponse copy = BeanCopyUtil.copy(check.getRepData(), CaptchaVerifyResultResponse.class);
        return ResultUtils.success(copy);
    }

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/image/{keyword}")
    @SaIgnore
    public ResponseEntity<Void> getImage(@PathVariable String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return ResponseEntity.badRequest().build();
        }
        try {

            String imageUrl = imageProxyService.getAndUploadImage(keyword);

            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(imageUrl))
                    .cacheControl(CacheControl.maxAge(Duration.ofHours(1)))
                    .build();

        } catch (Exception e) {
             log.error("Image proxy failed for keyword: {}", keyword, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


}
