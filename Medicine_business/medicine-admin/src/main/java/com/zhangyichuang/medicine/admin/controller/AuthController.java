package com.zhangyichuang.medicine.admin.controller;

import com.zhangyichuang.medicine.admin.model.request.AdminPasswordChangeRequest;
import com.zhangyichuang.medicine.admin.model.request.AdminPhoneVerificationCodeSendRequest;
import com.zhangyichuang.medicine.admin.model.request.AdminProfileUpdateRequest;
import com.zhangyichuang.medicine.admin.model.request.AdminUserPhoneChangeRequest;
import com.zhangyichuang.medicine.admin.service.AuthService;
import com.zhangyichuang.medicine.common.core.base.AjaxResult;
import com.zhangyichuang.medicine.common.redis.annotation.PreventDuplicateSubmit;
import com.zhangyichuang.medicine.common.security.annotation.Anonymous;
import com.zhangyichuang.medicine.common.security.base.BaseController;
import com.zhangyichuang.medicine.common.security.entity.AuthTokenVo;
import com.zhangyichuang.medicine.common.security.utils.SecurityUtils;
import com.zhangyichuang.medicine.model.request.LoginRequest;
import com.zhangyichuang.medicine.model.request.RefreshRequest;
import com.zhangyichuang.medicine.model.vo.CurrentUserInfoVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

/**
 * 管理端认证控制器。
 * <p>
 * 提供登录、刷新令牌、当前用户信息与权限查询、登出能力。
 * </p>
 */
@Slf4j
@RestController
@Tag(name = "认证接口", description = "注册、登录、刷新、当前用户")
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController extends BaseController {

    private final AuthService authService;


    /**
     * 登录
     *
     * @param request 请求参数
     * @return 登录结果
     */
    @Anonymous
    @Operation(summary = "登录", description = "登录成功返回访问令牌与刷新令牌")
    @PostMapping("/login")
    public AjaxResult<AuthTokenVo> login(@Valid @RequestBody LoginRequest request) {
        AuthTokenVo token = authService.login(request.getUsername(),
                request.getPassword(),
                request.getCaptchaVerificationId());
        return success(token);
    }

    /**
     * 刷新令牌
     *
     * @param request 刷新令牌参数
     * @return 刷新令牌结果
     */
    @Anonymous
    @Operation(summary = "刷新令牌", description = "使用刷新令牌获取新的访问令牌")
    @PostMapping("/refresh")
    public AjaxResult<AuthTokenVo> refresh(@Valid @RequestBody RefreshRequest request) {
        AuthTokenVo token = authService.refresh(request.getRefreshToken());
        return success(token);
    }

    /**
     * 获取当前用户信息
     *
     * @return 当前用户信息
     */
    @Operation(summary = "获取当前用户信息", description = "根据认证上下文返回当前登录用户信息")
    @GetMapping("/currentUser")
    public AjaxResult<CurrentUserInfoVo> currentUser() {
        CurrentUserInfoVo vo = authService.currentUserInfo();
        return success(vo);
    }

    /**
     * 修改当前登录用户资料。
     *
     * @param request 修改资料请求
     * @return 修改结果
     */
    @Operation(summary = "修改当前登录用户资料", description = "修改当前登录管理员头像、昵称、真实姓名与邮箱")
    @PutMapping("/profile")
    @PreventDuplicateSubmit
    public AjaxResult<Void> updateCurrentUserProfile(@Valid @RequestBody AdminProfileUpdateRequest request) {
        authService.updateCurrentUserProfile(
                request.getAvatar(),
                request.getNickname(),
                request.getRealName(),
                request.getEmail()
        );
        return success();
    }

    /**
     * 发送当前登录用户修改手机号验证码。
     *
     * @param request 发送验证码请求
     * @return 发送结果
     */
    @Operation(summary = "发送修改手机号验证码", description = "完成滑动验证码后生成手机号验证码")
    @PostMapping("/phone/verification-code")
    @PreventDuplicateSubmit
    public AjaxResult<Void> sendPhoneVerificationCode(
            @Valid @RequestBody AdminPhoneVerificationCodeSendRequest request
    ) {
        authService.sendPhoneVerificationCode(request.getPhoneNumber(), request.getCaptchaVerificationId());
        return success();
    }

    /**
     * 修改当前登录用户手机号。
     *
     * @param request 修改手机号请求
     * @return 修改结果
     */
    @Operation(summary = "修改当前登录用户手机号", description = "校验手机验证码后修改当前登录管理员手机号")
    @PutMapping("/phone")
    @PreventDuplicateSubmit
    public AjaxResult<Void> changeCurrentUserPhone(@Valid @RequestBody AdminUserPhoneChangeRequest request) {
        authService.changeCurrentUserPhone(request.getPhoneNumber(), request.getVerificationCode());
        return success();
    }

    /**
     * 修改当前登录用户密码。
     *
     * @param request 修改密码请求
     * @return 修改结果
     */
    @Operation(summary = "修改当前登录用户密码", description = "校验原密码与滑动验证码后修改当前登录管理员密码")
    @PutMapping("/password")
    @PreventDuplicateSubmit
    public AjaxResult<Void> changeCurrentUserPassword(@Valid @RequestBody AdminPasswordChangeRequest request) {
        authService.changeCurrentUserPassword(
                request.getOldPassword(),
                request.getNewPassword(),
                request.getCaptchaVerificationId()
        );
        return success();
    }

    /**
     * 获取当前用户权限
     *
     * @return 当前用户权限集合
     */
    @Operation(summary = "获取当前用户权限", description = "返回当前登录用户的权限编码列表")
    @GetMapping("/permissions")
    public AjaxResult<Set<String>> currentUserPermissions() {
        return success(authService.currentUserPermissions());
    }

    /**
     * 用户登出并清理当前会话。
     *
     * @return 登出结果
     */
    @PostMapping("/logout")
    @Operation(summary = "登出", description = "用户登出")
    @PreventDuplicateSubmit
    public AjaxResult<Void> logout() {
        authService.logout(SecurityUtils.getToken());
        return success();
    }

}
