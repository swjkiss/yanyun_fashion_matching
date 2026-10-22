package com.yanyun.fashion.service;

import com.yanyun.fashion.common.BizException;
import com.yanyun.fashion.entity.SysUser;
import com.yanyun.fashion.mapper.SysUserMapper;
import com.yanyun.fashion.security.JwtUtil;
import com.yanyun.fashion.security.LoginUser;
import com.yanyun.fashion.vo.LoginVO;
import com.yanyun.fashion.vo.UserInfoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

/**
 * 认证服务：口令校验（JeecgBoot 的 MD5(用户名+明文+盐) 规则）、令牌签发、用户信息组装。
 *
 * @author yanyun backend
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    /** 小程序免密登录的默认账号。 */
    private static final String DEFAULT_GUEST = "demo";

    private final SysUserMapper userMapper;

    private final JwtUtil jwtUtil;

    /**
     * 账号密码登录。
     *
     * @param username 登录名
     * @param password 明文口令
     * @return 令牌与用户信息
     */
    public LoginVO login(String username, String password) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new BizException("请输入用户名和密码");
        }
        SysUser user = userMapper.selectByUsername(username.trim());
        if (user == null) {
            throw new BizException("用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BizException("账号已被冻结，请联系管理员");
        }
        String expected = encrypt(user.getUsername(), password, user.getSalt());
        if (!expected.equalsIgnoreCase(user.getPassword())) {
            throw new BizException("用户名或密码错误");
        }
        log.info("[登录] {} ({}) 登录成功", user.getUsername(), user.getId());
        return buildLogin(user);
    }

    /**
     * 小程序免密登录：演示环境按用户名直接换取令牌，缺省用 demo 账号。
     *
     * @param username 用户名，可空
     * @return 令牌与用户信息
     */
    public LoginVO guestLogin(String username) {
        String target = StringUtils.hasText(username) ? username.trim() : DEFAULT_GUEST;
        SysUser user = userMapper.selectByUsername(target);
        if (user == null) {
            user = userMapper.selectByUsername(DEFAULT_GUEST);
        }
        if (user == null) {
            throw new BizException("演示账号不存在，请先初始化数据库");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BizException("账号已被冻结");
        }
        return buildLogin(user);
    }

    /**
     * 按用户ID取用户信息（令牌续签场景）。
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    public UserInfoVO userInfo(String userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw BizException.unauthorized("用户不存在，请重新登录");
        }
        return toUserInfo(user);
    }

    /**
     * 组装登录结果。
     *
     * @param user 用户实体
     * @return 登录结果
     */
    private LoginVO buildLogin(SysUser user) {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(user.getId());
        loginUser.setUsername(user.getUsername());
        loginUser.setRealname(user.getRealname());
        loginUser.setAvatar(user.getAvatar());
        loginUser.setRole(user.getRole());

        LoginVO vo = new LoginVO();
        vo.setToken(jwtUtil.createToken(loginUser));
        vo.setUserInfo(toUserInfo(user));
        return vo;
    }

    /**
     * 实体 → 用户信息。
     *
     * @param user 用户实体
     * @return 用户信息
     */
    public UserInfoVO toUserInfo(SysUser user) {
        UserInfoVO vo = new UserInfoVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealname(user.getRealname());
        vo.setAvatar(user.getAvatar());
        vo.setRole(user.getRole());
        if ("admin".equalsIgnoreCase(user.getRole())) {
            vo.setIdentity("掌阁人");
        } else if ("demo".equalsIgnoreCase(user.getUsername())) {
            vo.setIdentity("江湖游侠");
        } else {
            vo.setIdentity("云游客");
        }
        return vo;
    }

    /**
     * JeecgBoot 口令算法：MD5(用户名 + 明文 + 盐)，小写十六进制。
     *
     * @param username 用户名
     * @param password 明文口令
     * @param salt     盐
     * @return 32 位小写十六进制
     */
    public static String encrypt(String username, String password, String salt) {
        String raw = (username == null ? "" : username)
                + (password == null ? "" : password)
                + (salt == null ? "" : salt);
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] bytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString().toLowerCase(Locale.ROOT);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("当前 JDK 不支持 MD5", e);
        }
    }
}
