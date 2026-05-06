package com.dog.usersystem.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.stereotype.Component;

import java.util.Date;

// JWT 工具类
@Component
public class JwtUtil {

    private static final String SECRET_KEY = "dog_secret";

    public static final long ACCESS_EXPIRE = 1000 * 60 * 30;

    public static final long REFRESH_EXPIRE = 1000L * 60 * 60 * 24 * 7;

    public static final String ACCESS_TOKEN = "access_token";

    public static final String REFRESH_TOKEN = "refresh_token";

    // 创建短期 token
    public static String createAccessToken(Long userId) {
        return JWT.create()
                .withClaim("userId", userId)
                .withClaim("type", ACCESS_TOKEN)
                .withExpiresAt(new Date(System.currentTimeMillis() + ACCESS_EXPIRE))
                .sign(Algorithm.HMAC256(SECRET_KEY));
    }

    // 创建长期 token
    public static String createRefreshToken(Long userId) {
        return JWT.create()
                .withClaim("userId", userId)
                .withClaim("type", REFRESH_TOKEN)
                .withExpiresAt(new Date(System.currentTimeMillis() + REFRESH_EXPIRE))
                .sign(Algorithm.HMAC256(SECRET_KEY));
    }

    // 验证是否是 accessToken
    public static boolean isAccessToken(String token) {
        return ACCESS_TOKEN.equals(JWT.decode(token).getClaim("type").asString());
    }

    // 验证是否是 refreshToken
    public static boolean isRefreshToken(String token) {
        return REFRESH_TOKEN.equals(JWT.decode(token).getClaim("type").asString());
    }

    // 验证 token 是否有效
    public static boolean verify(String token) {
        try {
            JWT.require(Algorithm.HMAC256(SECRET_KEY)).build().verify(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // 获取 userId
    public static Long getUserId(String token) {
        return JWT.decode(token).getClaim("userId").asLong();
    }

    // 获取剩余过期时间（秒）
    public static long getRemainingExpireSeconds(String token) {
        DecodedJWT decoded = JWT.decode(token);
        long expireTime = decoded.getExpiresAt().getTime();
        long now = System.currentTimeMillis();
        return (expireTime - now) / 1000;
    }
}
