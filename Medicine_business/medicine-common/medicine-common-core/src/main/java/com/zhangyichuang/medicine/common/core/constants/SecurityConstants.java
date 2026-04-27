package com.zhangyichuang.medicine.common.core.constants;

/**
 * @author Chuang
 * <p>
 * created on 2025/8/28
 */
public class SecurityConstants {

    public static final String CLAIM_KEY_SESSION_ID = "session";
    public static final String CLAIM_KEY_USERNAME = "username";

    /**
     * 接口白名单,设置后不需要认证直接可以访问，注意！一旦设置白名单系统中需要获取用户的信息将失效！
     */
    public static final String[] WHITELIST = {
            "/auth/login",
            "/captcha",
            "/auth/register",
            "/auth/refresh",
    };

    /**
     * 静态资源白名单
     */
    public static final String[] STATIC_RESOURCES_WHITELIST = {
            "/static/**",
            "/profile/**",
            "/**.html",
            "/**.css",
            "/**.js",
            "/favicon.ico"
    };
    /**
     * 接口文档接口白名单
     */
    public static final String[] SWAGGER_WHITELIST = {
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/**",
            "/webjars/**"
    };
}
