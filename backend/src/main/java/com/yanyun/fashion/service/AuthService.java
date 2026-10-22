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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

/**
 * 认证服务：口令校验（BCrypt；兼容历史 MD5 账号并在登录后自动升级）、令牌签发、用户信息组装。
 *
 * @author yanyun backend
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    /** 小程序免密登录的默认账号。 */
    private static final String DEFAULT_GUEST = "demo";

    /** BCrypt 哈希前缀（$2a$ / $2b$ / $2y$）。 */
    private static final String BCRYPT_PREFIX = "$2";

    private final SysUserMapper userMapper;

    private final JwtUtil jwtUtil;

    /** 口令编码器：BCrypt 自带随机盐，无需额外 salt 字段。 */
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

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
        if (!matches(user, password)) {
            throw new BizException("用户名或密码错误");
        }
        log.info("[登录] {} ({}) 登录成功", user.getUsername(), user.getId());
        return buildLogin(user);
    }

    /**
     * 校验口令。历史 MD5 账号校验通过后立即改写为 BCrypt，实现平滑迁移。
     *
     * @param user     用户
     * @param password 明文口令
     * @return 是否匹配
     */
    private boolean matches(SysUser user, String password) {
        String stored = user.getPassword();
        if (stored != null && stored.startsWith(BCRYPT_PREFIX)) {
            return passwordEncoder.matches(password, stored);
        }
        if (!legacyEncrypt(user.getUsername(), password, user.getSalt()).equalsIgnoreCase(stored)) {
            return false;
        }
        upgradeToBcrypt(user, password);
        return true;
    }

    /**
     * 把历史 MD5 口令升级为 BCrypt，失败只记录日志、不影响本次登录。
     *
     * @param user     用户
     * @param password 本次登录的明文口令
     */
    private void upgradeToBcrypt(SysUser user, String password) {
        try {
            SysUser update = new SysUser();
            update.setId(user.getId());
            update.setPassword(passwordEncoder.encode(password));
            update.setSalt("");
            userMapper.updateById(update);
            user.setPassword(update.getPassword());
            user.setSalt("");
            log.info("[登录] 用户 {} 的口令已从 MD5 升级为 BCrypt", user.getUsername());
        } catch (Exception e) {
            log.warn("[登录] 用户 {} 口令升级失败：{}", user.getUsername(), e.getMessage());
        }
    }

    /**
     * 小程序免密登录：固定签发演示账号（demo）令牌。
     *
     * <p>安全约束：不接受调用方传入的用户名。guest-login 是匿名接口，
     * 若按用户名直通签发令牌，任何人可冒充 sys_user 表中的任意账号
     * （包括 admin 角色账号），因此这里只允许默认演示账号。</p>
     *
     * @return 令牌与用户信息
     */
    public LoginVO guestLogin() {
        SysUser user = userMapper.selectByUsername(DEFAULT_GUEST);
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
     * 历史口令算法（JeecgBoot 兼容）：MD5(用户名 + 明文 + 盐)，小写十六进制。
     *
     * <p>仅用于校验存量 MD5 账号，登录成功后会被 {@link #upgradeToBcrypt} 改写为 BCrypt，
     * 新口令一律使用 BCrypt，不再产生 MD5。</p>
     *
     * @param username 用户名
     * @param password 明文口令
     * @param salt     盐
     * @return 32 位小写十六进制
     */
    private static String legacyEncrypt(String username, String password, String salt) {
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
