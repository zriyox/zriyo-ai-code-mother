package com.zriyo.aicodemother.wechatlogin;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "wechat")
@Data
public class WeChatConfig {
    private String appid;
    private String appsecret;
    private String redirectUri;
    private String scope;
}
