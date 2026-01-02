package com.zriyo.aicodemother.service.email;

import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.zriyo.aicodemother.exception.BusinessException;
import com.zriyo.aicodemother.exception.ErrorCode;
import com.zriyo.aicodemother.model.enums.EmailCaptchaType;
import com.zriyo.aicodemother.util.RedisUtils;
import com.zriyo.aicodemother.util.VirtualThreadUtils;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RateType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@Slf4j
public class EmailService {

    @Autowired
    private JavaMailSender javaMailSender;

    public static final String EMAIL_CODE = "emailCode:";
    public static final String EMAIL_CODE_NEXT_CODE = "emailCodeNex:";

    @Value("${spring.mail.username}")
    private String from;

    public void sendSimpleEmail(String to, String subject, String text) throws MessagingException {
        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setFrom(from);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(text,true);
        javaMailSender.send(message);
    }


    public void getEmailCode(String email, EmailCaptchaType emailCaptchaType) {
        // 1. 非空校验
        if (StrUtil.isBlank(email)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱不能为空");
        }

        if (!Validator.isEmail(email)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱格式错误");
        }
        String key = EmailService.EMAIL_CODE + emailCaptchaType.getKey() + "_" + email;
        String keyNex = EmailService.EMAIL_CODE_NEXT_CODE + emailCaptchaType.getKey() + "_" + email;

        long result = RedisUtils.rateLimiter(
                keyNex,
                RateType.PER_CLIENT,
                1,
                180
        );
        if (result == -1) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST);
        }
        String code = RandomUtil.randomNumbers(6);
        RedisUtils.setCacheObject(key, code, Duration.ofMinutes(10));
        VirtualThreadUtils.runAsync(() -> {
            try {
                sendSimpleEmail(email, "邮箱验证码", getHtmlEmail(code, emailCaptchaType.getDescription()));
            } catch (MessagingException e) {
                log.error("发送邮件失败：{}", e.getMessage());
            }
        });

    }

    public boolean checkEmailCode(String email, String emailCode, EmailCaptchaType register) {
        if (StrUtil.isBlank(email) || StrUtil.isBlank(emailCode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        String key = EmailService.EMAIL_CODE + register.getKey() + "_" + email;
        String code = RedisUtils.getCacheObject(key);
        if (code != null && code.equals(emailCode)) {
            return RedisUtils.deleteObject(key);
        }
        return false;
    }

    public String getHtmlEmail(String code, String title) {
        String platformUrl = "https://www.zriyo.com";

        // 使用 Java 15+ 文本块。如果是旧版本，请自行改为字符串拼接。
        return """
    <!DOCTYPE html>
    <html lang="zh-CN">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>%s</title>
        <style>
            /* 邮件客户端核心兼容 */
            .copy-area { -webkit-user-select: all; user-select: all; cursor: pointer; }
            @media only screen and (max-width: 600px) {
                .container { width: 100%% !important; border-radius: 0 !important; }
                .code-font { font-size: 36px !important; letter-spacing: 4px !important; }
            }
        </style>
    </head>
    <body style="margin: 0; padding: 0; background-color: #f5f5f7; font-family: -apple-system, system-ui, sans-serif;">
        <table width="100%%" border="0" cellspacing="0" cellpadding="0" style="background-color: #f5f5f7;">
            <tr>
                <td align="center" style="padding: 40px 15px;">
                    <table class="container" width="520" border="0" cellspacing="0" cellpadding="0" 
                           style="background-color: #ffffff; border-radius: 24px; overflow: hidden; box-shadow: 0 10px 30px rgba(0,0,0,0.05);">
                        
                        <tr>
                            <td align="center" style="padding: 48px 40px 32px 40px;">
                                <a href="%s" style="text-decoration: none;">
                                    <div style="font-size: 24px; font-weight: 700; color: #1d1d1f; letter-spacing: -1px; margin-bottom: 8px;">
                                        Zriyo<span style="color: #0071e3;">Code</span>
                                    </div>
                                </a>
                                <div style="font-size: 14px; color: #86868b; font-weight: 400;">%s</div>
                            </td>
                        </tr>

                        <tr>
                            <td align="center" style="padding: 0 40px 40px 40px;">
                                <h1 style="font-size: 26px; font-weight: 600; color: #1d1d1f; margin: 0 0 16px 0; letter-spacing: -0.5px;">
                                    验证您的登录
                                </h1>
                                <p style="font-size: 16px; line-height: 1.6; color: #424245; margin: 0 0 32px 0;">
                                    您好！请使用下方的验证码完成身份验证。
                                </p>

                                <table border="0" cellspacing="0" cellpadding="0">
                                    <tr>
                                        <td align="center" bgcolor="#f5f5f7" style="border: 1px dashed #d2d2d7; border-radius: 16px; padding: 28px 45px;">
                                            <span class="copy-area" style="font-size: 42px; font-weight: 800; color: #0071e3; letter-spacing: 8px; font-family: 'SF Pro Display', 'Roboto Mono', monospace;">
                                                %s
                                            </span>
                                        </td>
                                    </tr>
                                </table>
                                
                                <p style="font-size: 12px; color: #86868b; margin-top: 16px;">
                                    💡 提示：点击验证码可快速全选并复制
                                </p>

                                <div style="margin-top: 40px;">
                                    <a href="%s" style="background-color: #0071e3; color: #ffffff; padding: 14px 32px; border-radius: 12px; font-size: 15px; font-weight: 600; text-decoration: none; display: inline-block;">
                                        进入 ZriyoCode 平台
                                    </a>
                                </div>
                            </td>
                        </tr>

                        <tr>
                            <td align="center" style="padding: 32px 40px; background-color: #fafafa; border-top: 1px solid #f2f2f2;">
                                <p style="font-size: 12px; color: #86868b; line-height: 1.8; margin: 0;">
                                    此验证码将在 10 分钟后失效。<br>
                                    © 2025 ZriyoCode - 零代码应用生成平台<br>
                                    <a href="%s" style="color: #0071e3; text-decoration: none;">www.zriyo.com</a>
                                </p>
                            </td>
                        </tr>
                    </table>
                    
                    <p style="margin-top: 24px; font-size: 12px; color: #b4b4b8; text-align: center;">
                        如果您没有请求此邮件，请忽略。
                    </p>
                </td>
            </tr>
        </table>
    </body>
    </html>
    """.formatted(title, platformUrl, title, code, platformUrl, platformUrl);
    }
}
