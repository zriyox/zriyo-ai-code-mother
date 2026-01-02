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

        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()))
                .addPathPatterns("/**")
                .excludePathPatterns(excludePaths.toArray(new String[0]));
    }

    @Bean
    public SaCorsHandleFunction corsHandle() {
        return (req, res, sto) -> {
            res.setHeader("Access-Control-Allow-Origin", "*");
            res.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            res.setHeader("Access-Control-Max-Age", "3600");
            res.setHeader("Access-Control-Allow-Headers", "*");

            if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
                res.setStatus(200); // ✅ 必须设置状态码
            }
            SaRouter.match(SaHttpMethod.OPTIONS)
                    .free(r -> {
                        // ✅ 可以在这里添加日志，但不要修改响应
                        log.info("--------OPTIONS预检请求，已设置CORS头");
                    })
                    .back(); // ✅ 抛出异常，中断后续处理
        };
    }

}
