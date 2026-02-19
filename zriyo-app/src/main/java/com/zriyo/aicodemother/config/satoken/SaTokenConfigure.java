package com.zriyo.aicodemother.config.satoken;

import cn.dev33.satoken.fun.strategy.SaCorsHandleFunction;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaHttpMethod;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

@Configuration
@Slf4j
public class SaTokenConfigure implements WebMvcConfigurer {

    private final SaTokenProperties saTokenProperties;

    public SaTokenConfigure(SaTokenProperties saTokenProperties) {
        this.saTokenProperties = saTokenProperties;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        List<String> excludePaths = saTokenProperties.getExcludePaths();

        // 添加默认的排除路径
        List<String> defaultExcludes = Arrays.asList(
                "/auth/login",
                "/auth/register",
                "/captcha/**",
                "/health",
                "/actuator/**",
                "/error"
        );

        // 合并配置的排除路径和默认排除路径
        defaultExcludes.addAll(excludePaths);

        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()))
                .addPathPatterns("/**")
                .excludePathPatterns(defaultExcludes.toArray(new String[0]));
    }

    @Bean
    public SaCorsHandleFunction corsHandle() {
        return (req, res, sto) -> {
            res.setHeader("Access-Control-Allow-Origin", "*");
            res.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            res.setHeader("Access-Control-Max-Age", "3600");
            res.setHeader("Access-Control-Allow-Headers", "*");

            if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
                res.setStatus(200);
            }
            SaRouter.match(SaHttpMethod.OPTIONS)
                    .free(r -> {
                        log.info("--------OPTIONS预检请求，已设置CORS头");
                    })
                    .back();
        };
    }

}
