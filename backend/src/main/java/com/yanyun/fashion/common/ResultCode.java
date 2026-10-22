package com.yanyun.fashion.common;

/**
 * 统一状态码（与 docs/contract-api.md 0.4 一致）。
 *
 * @author yanyun backend
 */
public final class ResultCode {

    /** 成功。 */
    public static final int SUCCESS = 200;

    /** 未登录 / token 失效。 */
    public static final int UNAUTHORIZED = 401;

    /** 无权限。 */
    public static final int FORBIDDEN = 403;

    /** 业务异常。 */
    public static final int ERROR = 500;

    /** 资源不存在。 */
    public static final int NOT_FOUND = 404;

    private ResultCode() {
    }
}
