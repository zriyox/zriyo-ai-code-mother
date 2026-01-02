package com.zriyo.aicodemother.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "view.app")
public class AppViewConfig {

    /**
     * 应用预览访问前缀
     * 示例：
     *  dev  : /dev/api/app/view/
     *  prod : /api/app/view/
     */
    private String prefix;

}
