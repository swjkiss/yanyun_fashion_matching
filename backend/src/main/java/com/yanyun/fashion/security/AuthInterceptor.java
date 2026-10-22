package com.yanyun.fashion.security;

import com.yanyun.fashion.common.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 鉴权拦截器。
 *
 * <ul>
 *   <li>{@code /sys/**}（除登录、字典）与 {@code /fashion/**}：要求 admin 角色；</li>
 *   <li>{@code /app/**}（除 guest-login）：要求已登录，任意角色；</li>
 *   <li>未登录 / 过期统一 code=401，无权限 code=403。</li>
 * </ul>
 *
 * @author yanyun backend
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 预检请求直接放行
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        // 非 Controller 方法（静态资源等）不鉴权
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (StringUtils.hasLength(contextPath) && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }

        LoginUser user = resolveUser(request);

        if (path.startsWith("/app/")) {
            UserContext.set(user);
            return true;
        }

        if (!user.isAdmin()) {
            throw BizException.forbidden("无操作权限，请使用管理员账号登录");
        }
        UserContext.set(user);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }

    private LoginUser resolveUser(HttpServletRequest request) {
        String token = request.getHeader(JwtUtil.TOKEN_HEADER);
        if (!StringUtils.hasText(token)) {
            // 兼容 Authorization: Bearer xxx 形式
            String authorization = request.getHeader("Authorization");
            if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
                token = authorization.substring(7);
            }
        }
        if (!StringUtils.hasText(token)) {
            throw BizException.unauthorized("未登录，请先登录");
        }
        return jwtUtil.verify(token);
    }
}
