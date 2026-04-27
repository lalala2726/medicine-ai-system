package com.zhangyichuang.medicine.client.service;

import com.zhangyichuang.medicine.common.security.entity.AuthTokenVo;

/**
 * @author Chuang
 * <p>
 * created on 2025/10/25
 */
public interface AuthService {

    /**
     * 用户注册
     *
     * @param username 用户名
     * @param password 明文密码
     * @return 新用户ID
     */
    Long register(String username, String password);

    /**
     * 用户登录
     *
     * @param username              用户名
     * @param password              明文密码
     * @param captchaVerificationId 验证码校验凭证
     * @return 授权令牌
     */
    AuthTokenVo login(String username, String password, String captchaVerificationId);

    /**
     * 刷新令牌
     *
     * @param refreshToken 刷新令牌JWT
     * @return 新的访问令牌与原刷新令牌
     */
    AuthTokenVo refresh(String refreshToken);

    /**
     * 发送修改手机号验证码
     *
     * @param phoneNumber           新手机号
     * @param captchaVerificationId 验证码校验凭证
     */
    void sendPhoneVerificationCode(String phoneNumber, String captchaVerificationId);

    /**
     * 修改当前登录用户手机号
     *
     * @param phoneNumber       新手机号
     * @param verificationCode 手机验证码
     */
    void changePhone(String phoneNumber, String verificationCode);

    /**
     * 修改当前登录用户密码
     *
     * @param oldPassword           原密码
     * @param newPassword           新密码
     * @param captchaVerificationId 验证码校验凭证
     */
    void changePassword(String oldPassword, String newPassword, String captchaVerificationId);

    /**
     * 退出登录
     *
     * @param accessToken 访问令牌
     */
    void logout(String accessToken);
}
