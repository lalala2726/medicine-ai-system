package com.zhangyichuang.medicine.common.systemauth.core;

/**
 * 系统签名认证请求头常量。
 */
public final class SystemAuthHeaders {

    /**
     * 调用方 app_id 请求头。
     */
    public static final String X_AGENT_KEY = "X-Agent-Key";

    /**
     * 秒级 Unix 时间戳请求头。
     */
    public static final String X_AGENT_TIMESTAMP = "X-Agent-Timestamp";

    /**
     * 一次性随机串请求头。
     */
    public static final String X_AGENT_NONCE = "X-Agent-Nonce";

    /**
     * 签名版本请求头。
     */
    public static final String X_AGENT_SIGN_VERSION = "X-Agent-Sign-Version";

    /**
     * HMAC 签名结果请求头。
     */
    public static final String X_AGENT_SIGNATURE = "X-Agent-Signature";

    /**
     * 用户态鉴权请求头。
     */
    public static final String AUTHORIZATION = "Authorization";

    private SystemAuthHeaders() {
        throw new IllegalStateException("Utility class");
    }
}
