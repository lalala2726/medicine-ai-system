package com.zhangyichuang.medicine.common.security.token;

import com.zhangyichuang.medicine.common.core.constants.SecurityConstants;
import com.zhangyichuang.medicine.common.core.exception.AuthorizationException;
import com.zhangyichuang.medicine.common.security.config.SecurityProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import static com.zhangyichuang.medicine.common.core.enums.ResponseCode.ACCESS_TOKEN_INVALID;

/**
 * @author Chuang
 * <p>
 * created on 2025/8/28
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class JwtTokenProvider {
    private final SecurityProperties securityProperties;
    private SecretKey jwtSecretKey;

    @PostConstruct
    public void init() {
        String secret = securityProperties.getSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException(
                    "security.secret 未配置，无法初始化JWT密钥。请在 application.yml 中配置 security.secret（建议使用至少32字节或Base64编码后的密钥）。");
        }

        byte[] keyBytes;
        try {
            // 优先尝试按 Base64 解码，失败时再回退到明文 UTF-8 字节。
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (RuntimeException ignored) {
            // 非 Base64 时，使用明文的 UTF-8 字节。
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }

        // 确保长度满足HS256最小长度要求（>=32字节）。不足则对明文做SHA-256扩展；
        // 如果已是Base64随机字节通常会>=32字节，直接使用即可。
        if (keyBytes.length < 32) {
            try {
                MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
                keyBytes = sha256.digest(keyBytes);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("初始化JWT密钥失败: SHA-256不可用", e);
            }
        }

        this.jwtSecretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 创建JWT。不再包含tokenType。
     *
     * @param tokenId  令牌的唯一ID (对于访问令牌是accessTokenId，对于刷新令牌是refreshTokenId)
     * @param username 用户名
     * @return JWT字符串
     */
    public String createJwt(String tokenId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(SecurityConstants.CLAIM_KEY_SESSION_ID, tokenId);
        claims.put(SecurityConstants.CLAIM_KEY_USERNAME, username);
        return Jwts.builder()
                .setClaims(claims)
                .signWith(jwtSecretKey)
                .compact();
    }

    /**
     * 从JWT中解析Claims。
     * 遇到已知错误（过期、签名错误等）时抛出自定义异常。
     *
     * @param token JWT字符串
     * @return Claims对象，包含JWT的声明信息
     * @throws AuthorizationException 如果JWT无效 (例如格式错误、签名错误、过期)
     */
    public Claims getClaimsFromToken(String token) {
        try {
            Jws<Claims> jwsClaims = Jwts.parserBuilder()
                    .setSigningKey(jwtSecretKey)
                    .build()
                    .parseClaimsJws(token);
            return jwsClaims.getBody();
        } catch (ExpiredJwtException e) {
            log.warn("JWT已过期, message: {}", e.getMessage());
            throw new AuthorizationException(ACCESS_TOKEN_INVALID);
        } catch (UnsupportedJwtException e) {
            log.warn("不支持的JWT格式, message: {}", e.getMessage());
            throw new AuthorizationException(ACCESS_TOKEN_INVALID);
        } catch (MalformedJwtException e) {
            log.warn("JWT结构错误, message: {}", e.getMessage());
            throw new AuthorizationException(ACCESS_TOKEN_INVALID);
        } catch (IllegalArgumentException e) { // 通常是token为空或null
            log.warn("JWT claims字符串为空或无效参数, message: {}", e.getMessage());
            throw new AuthorizationException(ACCESS_TOKEN_INVALID);
        }
    }
}
