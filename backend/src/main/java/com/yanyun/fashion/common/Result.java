package com.yanyun.fashion.common;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 统一响应体（JeecgBoot 规范）。
 *
 * <pre>
 * { "success": true, "message": "", "code": 200, "result": {}, "timestamp": 1763000000000 }
 * </pre>
 *
 * @param <T> 业务数据类型
 * @author yanyun backend
 */
@Data
@NoArgsConstructor
@JsonPropertyOrder({"success", "message", "code", "result", "timestamp"})
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 业务是否成功。 */
    private boolean success;

    /** 失败原因 / 成功提示。 */
    private String message = "";

    /** 状态码，见 {@link ResultCode}。 */
    private int code;

    /** 业务数据。 */
    private T result;

    /** 服务器毫秒时间戳。 */
    private long timestamp = System.currentTimeMillis();

    public Result(boolean success, String message, int code, T result) {
        this.success = success;
        this.message = message == null ? "" : message;
        this.code = code;
        this.result = result;
        this.timestamp = System.currentTimeMillis();
    }

    public static <T> Result<T> ok() {
        return new Result<T>(true, "", ResultCode.SUCCESS, null);
    }

    public static <T> Result<T> ok(T data) {
        return new Result<T>(true, "", ResultCode.SUCCESS, data);
    }

    public static <T> Result<T> ok(String message, T data) {
        return new Result<T>(true, message, ResultCode.SUCCESS, data);
    }

    public static <T> Result<T> error(String message) {
        return new Result<T>(false, message, ResultCode.ERROR, null);
    }

    public static <T> Result<T> error(int code, String message) {
        return new Result<T>(false, message, code, null);
    }
}
