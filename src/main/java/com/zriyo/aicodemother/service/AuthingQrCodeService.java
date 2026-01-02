package com.zriyo.aicodemother.service;

import cn.authing.sdk.java.client.AuthenticationClient;
import cn.authing.sdk.java.client.BaseClient;
import cn.authing.sdk.java.dto.*;
import cn.authing.sdk.java.enums.AuthMethodEnum;
import cn.authing.sdk.java.model.AuthenticationClientOptions;
import cn.authing.sdk.java.model.AuthingRequestConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zriyo.aicodemother.exception.BusinessException;
import com.zriyo.aicodemother.exception.ErrorCode;
import com.zriyo.aicodemother.mapper.UserMapper;
import com.zriyo.aicodemother.model.vo.LoginUserVO;
import com.zriyo.aicodemother.model.vo.QrCodeSession;
import com.zriyo.aicodemother.util.UserAuthUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Base64;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthingQrCodeService {

    @Value("${authing.app-id}")
    private String appId;

    @Value("${authing.app-secret}")
    private String appSecret;

    @Value("${authing.app-host}")
    private String appHost;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UserMapper userMapper;


    private final UserService userService;


    // 1. 生成二维码
    public GeneQRCodeDataDto generateWechatMiniProgramQrCode() throws Exception {
        AuthenticationClient client = createAuthClient();
        GenerateQrcodeDto req = new GenerateQrcodeDto();
        req.setType(GenerateQrcodeDto.Type.WECHAT_MINIPROGRAM);
        GeneQRCodeRespDto geneQRCodeRespDto = client.geneQrCode(req);
        if (geneQRCodeRespDto.getStatusCode() != 200) {
            log.error("生成二维码失败: {}", geneQRCodeRespDto.getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成二维码失败");
        }
        return geneQRCodeRespDto.getData();
    }

    // 2. 轮询状态 + ticket 换 token + 解析 id_token
    public QrCodeSession checkQrCodeStatus(String qrcodeId) {
        try {
            AuthenticationClient client = createAuthClient();
            CheckQrcodeStatusDto req = new CheckQrcodeStatusDto();
            req.setQrcodeId(qrcodeId);
            AuthingRequestConfig config = new AuthingRequestConfig();
            config.setUrl("/api/v3/check-qrcode-status");
            config.setBody(req);
            config.setMethod("GET");
            String response = client.request(config);
            CheckQRCodeStatusDataDto data = BaseClient.deserialize(response, CheckQRCodeStatusDataDto.class);
            String status;
            if (data.getStatus() == null) {
                status = CheckQRCodeStatusDataDto.Status.EXPIRED.getValue();
            } else {
                status = data.getStatus().getValue();
            }
            QrCodeSession qrCodeSession = new QrCodeSession();
            qrCodeSession.setQrcodeId(qrcodeId);
            if (!"AUTHORIZED".equals(status)) {
                qrCodeSession.setState(status);
                return qrCodeSession;
            }

            String ticket = data.getTicket();
            if (ticket == null || ticket.isEmpty()) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR);
            }

            // 用 ticket 换 tokenSet
            ExchangeTokenSetWithQRcodeTicketDto exchangeReq = new ExchangeTokenSetWithQRcodeTicketDto();
            exchangeReq.setTicket(ticket);
            exchangeReq.setClientId(appId);
            exchangeReq.setClientSecret(appSecret);

            LoginTokenRespDto tokenResp = client.exchangeTokenSetWithQrCodeTicket(exchangeReq);
            if (tokenResp.getStatusCode() != 200 || tokenResp.getData() == null) {
                log.error("换 token 失败: {}", tokenResp.getMessage());
                throw new BusinessException(ErrorCode.SYSTEM_ERROR);
            }

            LoginTokenResponseDataDto tokenSet = tokenResp.getData();
            String idToken = tokenSet.getIdToken();

            // 解析 id_token 获取用户信息
            JsonNode userInfo = parseIdToken(idToken);
            if (userInfo == null) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR);
            }

            AuthenticationClientOptions clientOptions = new AuthenticationClientOptions();
            clientOptions.setAppId(appId);
            clientOptions.setAppSecret(
                    appSecret
            );

            clientOptions.setAppHost(appHost);
            clientOptions.setAccessToken(tokenSet.getAccessToken());
            AuthenticationClient clientTwo = new AuthenticationClient(clientOptions);
            UserSingleRespDto tokenUserInfo = clientTwo.getProfile(new GetProfileDto());
            // 本地 系统用户 id
            LoginUserVO orCreateLocalUser = userService.findOrCreateLocalUser(tokenUserInfo.getData().getUserId(), tokenUserInfo.getData().getPhone(), tokenUserInfo.getData().getPhoto(), tokenUserInfo.getData().getName());
            UserAuthUtil.userLogin(orCreateLocalUser.getId(), orCreateLocalUser);
            orCreateLocalUser.setToken(UserAuthUtil.getTokenValue());
            qrCodeSession.setUser(orCreateLocalUser);
            qrCodeSession.setState(status);
            return qrCodeSession;

        } catch (Exception e) {
            log.error("二维码状态查询失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统错误");
        }
    }

    // 解析 id_token（JWT）
    private JsonNode parseIdToken(String idToken) {
        try {
            // JWT 由三部分组成：header.payload.signature
            String[] parts = idToken.split("\\.");
            if (parts.length < 2) return null;

            String payload = parts[1];
            // 补齐 Base64 padding
            payload = padBase64(payload);
            byte[] decoded = Base64.getUrlDecoder().decode(payload);
            return objectMapper.readTree(decoded);
        } catch (Exception e) {
            log.warn("解析 id_token 失败", e);
            return null;
        }
    }

    private String padBase64(String base64) {
        int missing = 4 - (base64.length() % 4);
        if (missing == 4) return base64;
        return base64 + "====".substring(0, missing);
    }

    private AuthenticationClient createAuthClient() throws Exception {
        AuthenticationClientOptions options = new AuthenticationClientOptions();
        options.setAppId(appId);
        options.setAppSecret(appSecret);
        options.setAppHost(appHost);
        options.setTokenEndPointAuthMethod(AuthMethodEnum.CLIENT_SECRET_POST.getValue());
        return new AuthenticationClient(options);
    }
}
