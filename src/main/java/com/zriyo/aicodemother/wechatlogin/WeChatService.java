package com.zriyo.aicodemother.wechatlogin;




import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class WeChatService {


    @Autowired
    private WeChatConfig weChatConfig;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 生成授权 URL（用于跳转）
    public String buildAuthUrl(String state) {
        try {
            String encodedRedirectUri = URLEncoder.encode(weChatConfig.getRedirectUri(), StandardCharsets.UTF_8.toString());
            return "https://open.weixin.qq.com/connect/qrconnect?" +
                    "appid=" + weChatConfig.getAppid() +
                    "&redirect_uri=" + encodedRedirectUri +
                    "&response_type=code" +
                    "&scope=" + weChatConfig.getScope() +
                    "&state=" + state +
                    "#wechat_redirect";
        } catch (Exception e) {
            throw new RuntimeException("构建授权URL失败", e);
        }
    }

    // 用 code 换取 access_token
    public WeChatAccessToken getAccessToken(String code) {
        String url = "https://api.weixin.qq.com/sns/oauth2/access_token?" +
                "appid=" + weChatConfig.getAppid() +
                "&secret=" + weChatConfig.getAppsecret() +
                "&code=" + code +
                "&grant_type=authorization_code";

        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        try {
            WeChatAccessToken token = objectMapper.readValue(response.getBody(), WeChatAccessToken.class);
            if (token.getErrcode() != null && token.getErrcode() != 0) {
                log.error("获取access_token失败: {}", token.getErrmsg());
                throw new RuntimeException("微信错误: " + token.getErrmsg());
            }
            return token;
        } catch (Exception e) {
            log.error("解析access_token失败", e);
            throw new RuntimeException("解析access_token失败", e);
        }
    }

    // 获取用户信息（需要 snsapi_userinfo 权限）
    public WeChatUserInfo getUserInfo(String accessToken, String openid) {
        String url = "https://api.weixin.qq.com/sns/userinfo?" +
                "access_token=" + accessToken +
                "&openid=" + openid +
                "&lang=zh_CN";

        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        try {
            WeChatUserInfo userInfo = objectMapper.readValue(response.getBody(), WeChatUserInfo.class);
            if (userInfo.getErrcode() != null && userInfo.getErrcode() != 0) {
                log.warn("获取用户信息失败: {}", userInfo.getErrmsg());
                // 注意：snsapi_login 默认无权限获取 userinfo！
            }
            return userInfo;
        } catch (Exception e) {
            log.error("解析用户信息失败", e);
            throw new RuntimeException("解析用户信息失败", e);
        }
    }
}
