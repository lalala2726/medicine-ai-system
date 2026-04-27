package com.zhangyichuang.medicine.client.controller;

import com.zhangyichuang.medicine.client.model.request.PhoneVerificationCodeSendRequest;
import com.zhangyichuang.medicine.client.model.request.UserPasswordChangeRequest;
import com.zhangyichuang.medicine.client.model.request.UserPhoneChangeRequest;
import com.zhangyichuang.medicine.client.model.vo.AgreementConfigVo;
import com.zhangyichuang.medicine.client.service.AuthService;
import com.zhangyichuang.medicine.common.core.base.AjaxResult;
import com.zhangyichuang.medicine.common.redis.annotation.PreventDuplicateSubmit;
import com.zhangyichuang.medicine.common.security.agreement.AgreementConfig;
import com.zhangyichuang.medicine.common.security.agreement.AgreementConfigService;
import com.zhangyichuang.medicine.common.security.annotation.Anonymous;
import com.zhangyichuang.medicine.common.security.base.BaseController;
import com.zhangyichuang.medicine.common.security.entity.AuthTokenVo;
import com.zhangyichuang.medicine.model.request.LoginRequest;
import com.zhangyichuang.medicine.model.request.RefreshRequest;
import com.zhangyichuang.medicine.model.request.RegisterRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @author Chuang
 * <p>
 * created on 2025/10/25
 */
@Slf4j
@RestController
@Tag(name = "客户端认证接口", description = "用户注册、登录、刷新令牌、获取个人信息")
@RequestMapping("/auth")
public class AuthController extends BaseController {

    private final AuthService authService;
    private final AgreementConfigService agreementConfigService;

    public AuthController(AuthService authService, AgreementConfigService agreementConfigService) {
        this.authService = authService;
        this.agreementConfigService = agreementConfigService;
    }

    /**
     * 登录
     *
     * @param request 请求参数
     * @return 登录结果
     */
    @Anonymous
    @Operation(summary = "登录", description = "登录成功返回访问令牌与刷新令牌")
    @PostMapping("/login")
    public AjaxResult<AuthTokenVo> login(@RequestBody LoginRequest request) {
        AuthTokenVo token = authService.login(request.getUsername(),
                request.getPassword(),
                request.getCaptchaVerificationId());
        return success(token);
    }

    /**
     * 注册
     *
     * @param request 登录参数
     * @return 注册结果
     */
    @Anonymous
    @Operation(summary = "注册", description = "注册新用户，返回用户ID")
    @PostMapping("/register")
    public AjaxResult<Long> register(@RequestBody RegisterRequest request) {
        Long userId = authService.register(request.getUsername(), request.getPassword());
        return success(userId);
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
    public AjaxResult<AuthTokenVo> refresh(@RequestBody RefreshRequest request) {
        AuthTokenVo token = authService.refresh(request.getRefreshToken());
        return success(token);
    }

    /**
     * 发送修改手机号验证码。
     *
     * @param request 发送验证码请求
     * @return 发送结果
     */
    @Operation(summary = "发送修改手机号验证码", description = "完成滑动验证码后生成手机号验证码并输出到后端日志")
    @PostMapping("/phone/verification-code")
    @PreventDuplicateSubmit
    public AjaxResult<Void> sendPhoneVerificationCode(@Validated @RequestBody PhoneVerificationCodeSendRequest request) {
        authService.sendPhoneVerificationCode(request.getPhoneNumber(), request.getCaptchaVerificationId());
        return success();
    }

    /**
     * 修改当前登录用户手机号。
     *
     * @param request 修改手机号请求
     * @return 修改结果
     */
    @Operation(summary = "修改当前登录用户手机号", description = "校验手机验证码后修改当前登录用户手机号")
    @PutMapping("/phone")
    @PreventDuplicateSubmit
    public AjaxResult<Void> changePhone(@Validated @RequestBody UserPhoneChangeRequest request) {
        authService.changePhone(request.getPhoneNumber(), request.getVerificationCode());
        return success();
    }

    /**
     * 修改当前登录用户密码。
     *
     * @param request 修改密码请求
     * @return 修改结果
     */
    @Operation(summary = "修改当前登录用户密码", description = "校验原密码与滑动验证码后修改当前登录用户密码")
    @PutMapping("/password")
    @PreventDuplicateSubmit
    public AjaxResult<Void> changePassword(@Validated @RequestBody UserPasswordChangeRequest request) {
        authService.changePassword(
                request.getOldPassword(),
                request.getNewPassword(),
                request.getCaptchaVerificationId()
        );
        return success();
    }

    /**
     * 获取软件协议配置。
     *
     * @return 软件协议配置
     */
    @Anonymous
    @Operation(summary = "获取软件协议配置", description = "返回客户端登录页展示的软件协议与隐私协议 Markdown 内容")
    @GetMapping("/agreement")
    public AjaxResult<AgreementConfigVo> getAgreementConfig() {
        AgreementConfig config = agreementConfigService.getAgreementConfig();
        AgreementConfigVo vo = new AgreementConfigVo();
        vo.setSoftwareAgreementMarkdown(config.getSoftwareAgreementMarkdown());
        vo.setPrivacyAgreementMarkdown(config.getPrivacyAgreementMarkdown());
        vo.setUpdatedTime(config.getUpdatedTime());
        return success(vo);
    }

    /**
     * 退出登录
     *
     * @param accessToken 访问令牌
     * @return 退出结果
     */
    @Operation(summary = "退出登录", description = "清理用户会话信息")
    @PostMapping("/logout")
    @PreventDuplicateSubmit
    public AjaxResult<Void> logout(@RequestHeader("Authorization") String accessToken) {
        if (accessToken.startsWith("Bearer ")) {
            accessToken = accessToken.substring(7);
        }
        authService.logout(accessToken);
        return success();
    }

}
