package com.yanyun.fashion.config;

import com.yanyun.fashion.security.AuthInterceptor;
import com.yanyun.fashion.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置：静态资源映射、鉴权拦截器、全局跨域。
 *
 * <ul>
 *   <li>{@code /images/**} → backend/upload/images/（目录不存在时自动创建，不影响启动）；</li>
 *   <li>拦截 /sys/**、/fashion/**、/app/**，放行登录与字典接口；</li>
 *   <li>跨域：Access-Control-Allow-Origin: *，放行 X-Access-Token 头。</li>
 * </ul>
 *
 * @author yanyun backend
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    private final StorageService storageService;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/sys/**", "/fashion/**", "/app/**")
                .excludePathPatterns(
                        "/sys/login",
                        "/sys/dict/**",
                        "/app/guest-login",
                        // 浏览类接口允许匿名：未登录也能看图鉴、看搭配详情（收藏态返回 null）
                        "/app/home",
                        "/app/fashion/**",
                        "/app/outfits",
                        "/app/outfit/**",
                        "/error",
                        "/error/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/images/**")
                .addResourceLocations(storageService.resourceLocation())
                .setCachePeriod(3600);
    }

    /**
     * 全局跨域过滤器（对静态资源、错误响应同样生效）。
     *
     * @param allowedOrigin 允许的来源
     * @return CORS 过滤器
     */
    @Bean
    public CorsFilter corsFilter(@Value("${app.cors.allowed-origin-pattern:*}") String allowedOrigin) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedOrigin(allowedOrigin);
        configuration.addAllowedHeader("*");
        configuration.addAllowedMethod("*");
        configuration.addExposedHeader("X-Access-Token");
        configuration.addExposedHeader("Content-Disposition");
        configuration.setMaxAge(3600L);
        configuration.setAllowCredentials(false);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return new CorsFilter(source);
    }
}
