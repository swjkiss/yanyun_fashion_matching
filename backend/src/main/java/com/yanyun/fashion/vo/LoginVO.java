package com.yanyun.fashion.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 登录结果（docs/contract-api.md 1.1）。
 *
 * @author yanyun backend
 */
@Data
public class LoginVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** JWT，后续请求放入 X-Access-Token 头。 */
    private String token;

    private UserInfoVO userInfo;
}
