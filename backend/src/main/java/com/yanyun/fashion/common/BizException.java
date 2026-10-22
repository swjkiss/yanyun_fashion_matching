package com.yanyun.fashion.common;

/**
 * 业务异常：message 为可直接展示给终端用户的中文文案。
 *
 * @author yanyun backend
 */
public class BizException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int code;

    public BizException(String message) {
        super(message);
        this.code = ResultCode.ERROR;
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static BizException unauthorized(String message) {
        return new BizException(ResultCode.UNAUTHORIZED, message);
    }

    public static BizException forbidden(String message) {
        return new BizException(ResultCode.FORBIDDEN, message);
    }

    public static BizException notFound(String message) {
        return new BizException(ResultCode.NOT_FOUND, message);
    }
}
