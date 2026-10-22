package com.yanyun.fashion.security;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 登录态用户（由 JWT 解析得到，不落库缓存）。
 *
 * @author yanyun backend
 */
@Data
@NoArgsConstructor
public class LoginUser implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户ID，如 u1001。 */
    private String userId;

    /** 登录名。 */
    private String username;

    /** 昵称。 */
    private String realname;

    /** 头像相对路径。 */
    private String avatar;

    /** admin / user。 */
    private String role;

    public boolean isAdmin() {
        return "admin".equalsIgnoreCase(role);
    }

    /** 展示用名称：优先昵称，其次登录名。 */
    public String displayName() {
        return realname == null || realname.isEmpty() ? username : realname;
    }
}
