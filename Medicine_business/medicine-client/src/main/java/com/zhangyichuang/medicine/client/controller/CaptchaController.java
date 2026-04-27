package com.zhangyichuang.medicine.client.controller;

import cloud.tianai.captcha.application.vo.ImageCaptchaVO;
import cloud.tianai.captcha.common.response.ApiResponse;
import com.zhangyichuang.medicine.common.captcha.model.CaptchaCheckRequest;
import com.zhangyichuang.medicine.common.captcha.model.CaptchaVerificationResponse;
import com.zhangyichuang.medicine.common.captcha.service.CaptchaService;
import com.zhangyichuang.medicine.common.core.base.AjaxResult;
import com.zhangyichuang.medicine.common.core.enums.ResponseCode;
import com.zhangyichuang.medicine.common.security.annotation.Anonymous;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * 客户端验证码控制器。
 *
 * @author Chuang
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/captcha")
@Tag(name = "客户端验证码接口", description = "客户端滑块验证码接口")
public class CaptchaController {

    /**
     * 验证码服务。
     */
    private final CaptchaService captchaService;

    /**
     * 生成滑块验证码 challenge。
     *
     * @return 滑块验证码数据
     */
    @Anonymous
    @GetMapping("/gen")
    @Operation(summary = "生成滑块验证码", description = "生成客户端滑块验证码 challenge")
    public AjaxResult<ImageCaptchaVO> generateSliderCaptcha() {
        return convertCaptchaResponse(captchaService.generateSliderCaptcha());
    }

    /**
     * 校验滑块验证码轨迹。
     *
     * @param request 校验请求
     * @return 登录阶段消费的验证码校验凭证
     */
    @Anonymous
    @PostMapping("/check")
    @Operation(summary = "校验滑块验证码", description = "校验客户端滑块验证码轨迹")
    public AjaxResult<CaptchaVerificationResponse> checkSliderCaptcha(@RequestBody CaptchaCheckRequest request) {
        return convertCaptchaResponse(captchaService.checkSliderCaptcha(request));
    }

    /**
     * 将 tianai 验证码响应转换为项目统一 AjaxResult。
     *
     * @param captchaResponse tianai 验证码响应
     * @return 项目统一响应
     */
    private <T> AjaxResult<T> convertCaptchaResponse(ApiResponse<T> captchaResponse) {
        if (captchaResponse != null && captchaResponse.isSuccess()) {
            return AjaxResult.success(captchaResponse.getData());
        }
        if (captchaResponse == null) {
            return AjaxResult.error("验证码服务返回为空");
        }
        return AjaxResult.error(ResponseCode.PARAM_ERROR.getCode(), StringUtils.hasText(captchaResponse.getMsg())
                ? captchaResponse.getMsg() : "验证码校验失败");
    }
}
