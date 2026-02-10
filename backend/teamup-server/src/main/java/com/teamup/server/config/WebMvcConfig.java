package com.teamup.server.config;

import com.teamup.server.common.security.PermissionInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * 静态资源映射配置和拦截器配置
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${file.upload.path:/var/uploads}")
    private String uploadPath;

    private final PermissionInterceptor permissionInterceptor;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 只暴露 public 目录（头像/封面等可公开资源）
        // 团队文件共享等私密文件存放在 private 目录，通过鉴权 API 访问，避免 /uploads 直链泄露
        String location = Paths.get(uploadPath, "public").toUri().toString();
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(permissionInterceptor)
                .addPathPatterns("/api/teams/**")  // 拦截所有团队相关接口
                .excludePathPatterns(
                        "/api/teams",  // 排除团队列表接口（不需要特定团队权限）
                        "/auth/**",    // 排除认证接口
                        "/uploads/**"  // 排除静态资源
                );
    }
}
