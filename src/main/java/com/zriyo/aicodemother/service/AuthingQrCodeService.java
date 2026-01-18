package com.zriyo.aicodemother.service;

import cn.authing.sdk.java.client.AuthenticationClient;
import cn.authing.sdk.java.client.BaseClient;
import cn.authing.sdk.java.dto.*;
import cn.authing.sdk.java.enums.AuthMethodEnum;
import cn.authing.sdk.java.model.AuthenticationClientOptions;
import cn.authing.sdk.java.model.AuthingRequestConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.type.LogicalType;
import com.zriyo.aicodemother.exception.BusinessException;
import com.zriyo.aicodemother.exception.ErrorCode;
import com.zriyo.aicodemother.model.vo.LoginUserVO;
import com.zriyo.aicodemother.model.vo.QrCodeSession;
import com.zriyo.aicodemother.util.UserAuthUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

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

    private final ObjectMapper objectMapper;

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

    // 2. 前端轮询状态接口 + 如果前端扫码 第三方状态改变 ticket 换 token 第三方平台为了安全
    // + 解析 id_token 后续有存在登入 查到用户的具体信息  不存在注册 并且返回当前系统的 token
    // 3.微信登入是对方回调我们的接口 并且携带信息
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

            // === 以下为关键修改：不再使用 clientTwo.getProfile()，改为手动调用 + 安全反序列化 ===
            String accessToken = tokenSet.getAccessToken();
            AuthingRequestConfig profileConfig = new AuthingRequestConfig();
            profileConfig.setUrl("/api/v3/get-profile");
            profileConfig.setMethod("GET");
            // 必须使用可变 Map！
            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Bearer " + accessToken);
            profileConfig.setHeaders(headers);

            String profileResponse = client.request(profileConfig);

            // 创建安全的 ObjectMapper，允许空字符串转为 null（避免 gender="" 导致枚举反序列化失败）
            objectMapper.coercionConfigFor(LogicalType.Enum)
                    .setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsNull);

            UserSingleRespDto tokenUserInfo = objectMapper.readValue(profileResponse, UserSingleRespDto.class);

            if (tokenUserInfo == null || tokenUserInfo.getData() == null) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "未能获取有效用户信息");
            }

            String userId = tokenUserInfo.getData().getUserId();
            if (userId == null || userId.trim().isEmpty()) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "用户ID缺失");
            }

            // 本地系统用户 id
            LoginUserVO orCreateLocalUser = userService.findOrCreateLocalUser(
                    userId,
                    tokenUserInfo.getData().getPhone(),
                    tokenUserInfo.getData().getPhoto(),
                    tokenUserInfo.getData().getName()
            );
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
