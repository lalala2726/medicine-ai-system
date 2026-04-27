package com.zhangyichuang.medicine.common.systemauth.core;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * 系统签名计算与比对工具。
 */
@Component
public class SystemAuthSigner {

    /**
     * 计算 Base64URL(HMAC_SHA256(secret, canonical)).
     *
     * @param secret    系统密钥
     * @param canonical 待签名原文
     * @return Base64URL 编码后的签名值
     */
    public String sign(String secret, String canonical) {
        if (secret == null || canonical == null) {
            throw new IllegalArgumentException("secret and canonical must not be null");
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("HmacSHA256 algorithm not available", ex);
        }
    }

    /**
     * 常量时间字符串比较。
     *
     * @param left  左值
     * @param right 右值
     * @return true 表示两个签名完全一致
     */
    public boolean constantTimeEquals(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        byte[] leftBytes = left.getBytes(StandardCharsets.UTF_8);
        byte[] rightBytes = right.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(leftBytes, rightBytes);
    }
}
