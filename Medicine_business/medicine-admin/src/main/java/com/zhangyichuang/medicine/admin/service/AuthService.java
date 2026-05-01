package com.zhangyichuang.medicine.admin.service;

import com.zhangyichuang.medicine.common.security.entity.AuthTokenVo;
import com.zhangyichuang.medicine.model.vo.CurrentUserInfoVo;

import java.util.Set;

/**
 * @author Chuang
 * <p>
 * created on 2025/8/28
 */
public interface AuthService {

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
     * 退出登录
     *
     * @param accessToken 访问令牌
     */
    void logout(String accessToken);

    /**
     * 获取当前登录用户信息
     */
    CurrentUserInfoVo currentUserInfo();

    /**
     * 修改当前登录用户资料。
     *
     * @param avatar   头像地址
     * @param nickname 昵称
     * @param realName 真实姓名
     * @param email    邮箱
     * @return 无返回值
     */
    void updateCurrentUserProfile(String avatar, String nickname, String realName, String email);

    /**
     * 发送当前登录用户修改手机号验证码。
     *
     * @param phoneNumber           新手机号
     * @param captchaVerificationId 滑动验证码校验凭证
     * @return 无返回值
     */
    void sendPhoneVerificationCode(String phoneNumber, String captchaVerificationId);

    /**
     * 修改当前登录用户手机号。
     *
     * @param phoneNumber       新手机号
     * @param verificationCode 手机验证码
     * @return 无返回值
     */
    void changeCurrentUserPhone(String phoneNumber, String verificationCode);

    /**
     * 修改当前登录用户密码。
     *
     * @param oldPassword           原登录密码
     * @param newPassword           新登录密码
     * @param captchaVerificationId 滑动验证码校验凭证
     * @return 无返回值
     */
    void changeCurrentUserPassword(String oldPassword, String newPassword, String captchaVerificationId);

    /**
     * 获取当前登录用户权限
     *
     * @return 权限列表
     */
    Set<String> currentUserPermissions();
}
