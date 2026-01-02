package com.zriyo.aicodemother.util;

import cn.dev33.satoken.stp.StpUtil;
import com.zriyo.aicodemother.exception.BusinessException;
import com.zriyo.aicodemother.exception.ErrorCode;
import com.zriyo.aicodemother.model.vo.LoginUserVO;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;

@Slf4j
@RequiredArgsConstructor
public class UserAuthUtil {

    /**
     * 存储当前用户信息
     *
     * @return 登录ID
     * @throws BusinessException 如果未登录
     */
    public static void userLogin(Long userId, LoginUserVO user) {
        StpUtil.login(userId);
        StpUtil.getSession().set("USER_INFO", user);
    }

    /**
     * 获取当前登录用户的 ID（Long 类型）
     *
     * @return 登录ID
     * @throws BusinessException 如果未登录
     */
    public static Long getLoginId() {
        Long userId = StpUtil.getLoginIdAsLong();
        if (!StpUtil.isLogin()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "用户未登录");
        }
        return userId;
    }


    /**
     * 判断当前会话是否已登录
     */
    public static boolean isLogin() {
        return StpUtil.isLogin();
    }

    /**
     * 退出当前登录
     */
    public static void logout() {
        StpUtil.logout();
    }


    public static LoginUserVO getLoginUserVO() {
        return (LoginUserVO) StpUtil.getSession().get("USER_INFO");
    }

    private static final String DEFAULT_TOKEN_HEADER = "Authorization"; // 默认请求头
    private static final String DEFAULT_TOKEN_PREFIX = "Authorization ";       // 默认前缀

    private static HttpServletRequest getRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "无法获取请求对象");
        }
        return attrs.getRequest();
    }

    /**
     * 从请求头或 Cookie 获取 token
     */
    public static String resolveToken(String headerName, String prefix) {
        HttpServletRequest request = getRequest();
        String hName = headerName != null ? headerName : DEFAULT_TOKEN_HEADER;
        String pre = prefix != null ? prefix : DEFAULT_TOKEN_PREFIX;

        // 请求头
        return getString(request, hName, pre);
    }

    /**
     * 从请求头或 Cookie 获取 token
     */
    public static String resolveToken() {
        HttpServletRequest request = getRequest();
        String hName = DEFAULT_TOKEN_HEADER;
        String pre = DEFAULT_TOKEN_PREFIX;

        return getString(request, hName, pre);
    }

    private static String getString(HttpServletRequest request, String hName, String pre) {
        // 请求头
        String token = request.getHeader(hName);
        if (token != null && token.startsWith(pre)) {
            return token.substring(pre.length());
        }

        // Cookie
        if (request.getCookies() != null) {
            token = Arrays.stream(request.getCookies())
                    .filter(c -> hName.equals(c.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);
        }

        if (token == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未获取到 token");
        }

        if (token.startsWith(pre)) {
            token = token.substring(pre.length());
        }

        return token;
    }

    /**
     * 从请求头或 Cookie 获取 token 并完成 Sa-Token 登录
     */
    public static void loginByRequestToken(String headerName, String prefix) {
        String token = resolveToken(headerName, prefix);
        Long userId = (Long) StpUtil.getLoginIdByToken(token);
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
    }

    /**
     * 从请求头或 Cookie 获取 token 并完成 Sa-Token 登录
     */
    public static Long loginByRequestToken() {
        String token = resolveToken();
        String userId = (String) StpUtil.getLoginIdByToken(token);
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return Long.parseLong(userId);
    }

    public static String getTokenValue() {
        return StpUtil.getTokenValue();
    }

    /**
     * 根据 token 获取用户
     */
    public static String getUserByToken(String token) {
        return StpUtil.getLoginIdByToken(token).toString();
    }


}
