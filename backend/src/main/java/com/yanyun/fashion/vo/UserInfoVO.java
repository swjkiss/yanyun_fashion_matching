package com.yanyun.fashion.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 当前登录用户信息（不包含口令与盐）。
 *
 * @author yanyun backend
 */
@Data
public class UserInfoVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    private String username;

    private String realname;

    private String avatar;

    /** admin / user。 */
    private String role;

    /** 江湖身份，小程序"我的"页展示用。 */
    private String identity = "江湖游侠";
}
