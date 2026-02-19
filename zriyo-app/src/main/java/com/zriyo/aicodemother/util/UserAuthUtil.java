package com.zriyo.aicodemother.util;

import cn.dev33.satoken.stp.StpUtil;
import com.zriyo.common.exception.BusinessException;
import com.zriyo.common.result.ErrorCode;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;

/**
 * 用户认证工具类
 * 基于 Sa-Token 实现用户登录、登出、权限验证等功能
 *
 * @author Zriyo AI
 * @since 2025-02-17
 */
@Slf4j
public class UserAuthUtil {

    private static final String DEFAULT_TOKEN_HEADER = "Authorization";
    private static final String DEFAULT_TOKEN_PREFIX = "Bearer ";
    private static final String USER_INFO_SESSION_KEY = "USER_INFO";

    /**
     * 用户登录并存储用户信息
     *
     * @param userId 用户ID
     * @param user   用户信息
     */
    public static void userLogin(Long userId, Object user) {
        StpUtil.login(userId);
        StpUtil.getSession().set(USER_INFO_SESSION_KEY, user);
    }

    /**
     * 获取当前登录用户的 ID
     *
     * @return 登录ID
     * @throws BusinessException 如果未登录
     */
    public static Long getLoginId() {
        if (!StpUtil.isLogin()) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return StpUtil.getLoginIdAsLong();
    }

    /**
     * 判断当前会话是否已登录
     *
     * @return 是否已登录
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

    /**
     * 获取当前登录用户信息
     *
     * @return 用户信息
     */
    public static Object getLoginUser() {
        return StpUtil.getSession().get(USER_INFO_SESSION_KEY);
    }

    /**
     * 获取当前请求对象
     *
     * @return HttpServletRequest
     */
    private static HttpServletRequest getRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "无法获取请求对象");
        }
        return attrs.getRequest();
    }

    /**
     * 从请求头或 Cookie 获取 token
     *
     * @param headerName 请求头名称
     * @param prefix     token 前缀
     * @return token值
     */
    public static String resolveToken(String headerName, String prefix) {
        HttpServletRequest request = getRequest();
        String hName = headerName != null ? headerName : DEFAULT_TOKEN_HEADER;
        String pre = prefix != null ? prefix : DEFAULT_TOKEN_PREFIX;

        return getTokenFromRequest(request, hName, pre);
    }

    /**
     * 从请求头或 Cookie 获取 token
     *
     * @return token值
     */
    public static String resolveToken() {
        HttpServletRequest request = getRequest();
        return getTokenFromRequest(request, DEFAULT_TOKEN_HEADER, DEFAULT_TOKEN_PREFIX);
    }

    /**
     * 从请求中提取 token
     */
    private static String getTokenFromRequest(HttpServletRequest request, String headerName, String prefix) {
        // 从请求头获取
        String token = request.getHeader(headerName);
        if (token != null && token.startsWith(prefix)) {
            return token.substring(prefix.length());
        }

        // 从 Cookie 获取
        if (request.getCookies() != null) {
            token = Arrays.stream(request.getCookies())
                    .filter(c -> headerName.equals(c.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);
        }

        if (token == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        if (token.startsWith(prefix)) {
            token = token.substring(prefix.length());
        }

        return token;
    }

    /**
     * 从请求头或 Cookie 获取 token 并完成 Sa-Token 登录
     *
     * @param headerName 请求头名称
     * @param prefix     token 前缀
     * @return 用户ID
     */
    public static Long loginByRequestToken(String headerName, String prefix) {
        String token = resolveToken(headerName, prefix);
        Object userId = StpUtil.getLoginIdByToken(token);
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return Long.parseLong(userId.toString());
    }

    /**
     * 从请求头或 Cookie 获取 token 并完成 Sa-Token 登录
     *
     * @return 用户ID
     */
    public static Long loginByRequestToken() {
        String token = resolveToken();
        Object userId = StpUtil.getLoginIdByToken(token);
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return Long.parseLong(userId.toString());
    }

    /**
     * 获取当前 token 值
     *
     * @return token值
     */
    public static String getTokenValue() {
        return StpUtil.getTokenValue();
    }

    /**
     * 根据 token 获取用户ID
     *
     * @param token token值
     * @return 用户ID
     */
    public static String getUserByToken(String token) {
        Object loginId = StpUtil.getLoginIdByToken(token);
        return loginId != null ? loginId.toString() : null;
    }
}
