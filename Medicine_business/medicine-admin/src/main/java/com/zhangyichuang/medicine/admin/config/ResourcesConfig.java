package com.zhangyichuang.medicine.admin.config;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 资源路径配置类
 * <p>
 * 该类用于配置 Spring Boot 的静态资源映射，支持本地存储文件访问和 API 文档访问
 *
 * @author Chuang
 * created 2025/2/19
 */
@Configuration
public class ResourcesConfig implements WebMvcConfigurer {

    /**
     * 配置静态资源访问路径
     */
    @Override
    public void addResourceHandlers(@NotNull ResourceHandlerRegistry registry) {
        configureStaticResources(registry);
    }

    /**
     * 配置静态资源（如 `static` 目录下的 CSS、JS、图片等）
     */
    private void configureStaticResources(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
    }

}
