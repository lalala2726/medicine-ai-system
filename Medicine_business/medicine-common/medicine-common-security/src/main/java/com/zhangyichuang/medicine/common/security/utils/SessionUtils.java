package com.zhangyichuang.medicine.common.security.utils;

import com.zhangyichuang.medicine.common.security.SessionManager;
import org.springframework.stereotype.Component;

/**
 * @author Chuang
 * <p>
 * created on 2025/9/26
 */
@Component
public class SessionUtils {

    private static SessionManager sessionManager;

    public SessionUtils(SessionManager sessionManager) {
        SessionUtils.sessionManager = sessionManager;
    }

    /**
     * 检查角色
     */
    public static boolean checkRole(String role) {
        return sessionManager.checkRole(role);
    }

    /**
     * 注销特定用户的登录
     */
    public static boolean logout(String username) {
        return sessionManager.logout(username);
    }

    /**
     * 通过特定刷新令牌注销此用户
     */
    public static void logoutByToken(String accessToken) {
        sessionManager.logoutByToken(accessToken);
    }

}
