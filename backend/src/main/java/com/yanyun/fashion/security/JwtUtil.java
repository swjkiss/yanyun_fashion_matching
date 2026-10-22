package com.yanyun.fashion.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.yanyun.fashion.common.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * JWT 工具（java-jwt），令牌通过请求头 X-Access-Token 传递。
 *
 * @author yanyun backend
 */
@Slf4j
@Component
public class JwtUtil {

    /** 请求头名称（JeecgBoot 约定）。 */
    public static final String TOKEN_HEADER = "X-Access-Token";

    /** 显式配置的密钥最小长度（HMAC-SHA256 建议不低于 256 bit）。 */
    private static final int MIN_SECRET_LENGTH = 32;

    private final Algorithm algorithm;

    private final JWTVerifier verifier;

    private final long expireMillis;

    private final String issuer;

    public JwtUtil(@Value("${app.jwt.secret:}") String secret,
                   @Value("${app.jwt.expire-hours:168}") long expireHours,
                   @Value("${app.jwt.issuer:yanyun-fashion}") String issuer) {
        this.algorithm = Algorithm.HMAC256(resolveSecret(secret));
        this.issuer = issuer;
        this.expireMillis = TimeUnit.HOURS.toMillis(expireHours);
        this.verifier = JWT.require(algorithm).withIssuer(issuer).build();
    }

    /**
     * 解析签名密钥。
     *
     * <p>密钥不得写入代码或配置文件：生产环境通过环境变量 APP_JWT_SECRET 注入。
     * 未配置时生成一次性随机密钥，保证本地开发开箱即用，但重启后令牌全部失效。</p>
     *
     * @param configured 配置值，可为空
     * @return 实际使用的密钥
     */
    private static String resolveSecret(String configured) {
        String value = configured == null ? "" : configured.trim();
        if (value.isEmpty()) {
            byte[] random = new byte[64];
            new SecureRandom().nextBytes(random);
            log.warn("[JWT] 未配置 app.jwt.secret / 环境变量 APP_JWT_SECRET，已生成临时随机密钥："
                    + "仅适用于本地开发，重启后令牌全部失效；生产环境必须显式配置");
            return Base64.getEncoder().encodeToString(random);
        }
        if (value.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                    "JWT 密钥过短：APP_JWT_SECRET 至少需要 " + MIN_SECRET_LENGTH + " 个字符");
        }
        return value;
    }

    /**
     * 生成令牌。
     *
     * @param user 登录用户
     * @return JWT 字符串
     */
    public String createToken(LoginUser user) {
        Date now = new Date();
        return JWT.create()
                .withIssuer(issuer)
                .withSubject(user.getUserId())
                .withIssuedAt(now)
                .withExpiresAt(new Date(now.getTime() + expireMillis))
                .withClaim("userId", user.getUserId())
                .withClaim("username", user.getUsername())
                .withClaim("realname", user.getRealname())
                .withClaim("avatar", user.getAvatar())
                .withClaim("role", user.getRole())
                .sign(algorithm);
    }

    /**
     * 校验并解析令牌。
     *
     * @param token JWT
     * @return 登录用户
     * @throws BizException 401 未登录或令牌失效
     */
    public LoginUser verify(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw BizException.unauthorized("未登录，请先登录");
        }
        try {
            DecodedJWT jwt = verifier.verify(token.trim());
            LoginUser user = new LoginUser();
            user.setUserId(jwt.getClaim("userId").asString());
            user.setUsername(jwt.getClaim("username").asString());
            user.setRealname(jwt.getClaim("realname").asString());
            user.setAvatar(jwt.getClaim("avatar").asString());
            user.setRole(jwt.getClaim("role").asString());
            if (user.getUserId() == null || user.getUserId().isEmpty()) {
                throw BizException.unauthorized("登录凭证无效，请重新登录");
            }
            return user;
        } catch (TokenExpiredException e) {
            throw BizException.unauthorized("登录已过期，请重新登录");
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[JWT] 校验失败: {}", e.getMessage());
            throw BizException.unauthorized("登录凭证无效，请重新登录");
        }
    }
}
