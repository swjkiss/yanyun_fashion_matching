package com.yanyun.fashion.security;

import com.yanyun.fashion.common.BizException;

/**
 * 当前请求的登录用户上下文（ThreadLocal，请求结束由拦截器清理）。
 *
 * @author yanyun backend
 */
public final class UserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<LoginUser>();

    private UserContext() {
    }

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    /**
     * 取当前登录用户，取不到即视为未登录。
     *
     * @return 登录用户
     */
    public static LoginUser require() {
        LoginUser user = HOLDER.get();
        if (user == null) {
            throw BizException.unauthorized("未登录，请先登录");
        }
        return user;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
