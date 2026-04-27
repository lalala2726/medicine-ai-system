package com.zhangyichuang.medicine.common.captcha.service;

import cloud.tianai.captcha.application.vo.ImageCaptchaVO;
import cloud.tianai.captcha.common.response.ApiResponse;
import com.zhangyichuang.medicine.common.captcha.model.CaptchaCheckRequest;
import com.zhangyichuang.medicine.common.captcha.model.CaptchaVerificationResponse;

/**
 * 验证码服务接口。
 *
 * @author Chuang
 */
public interface CaptchaService {

    /**
     * 生成滑块验证码 challenge。
     *
     * @return 滑块验证码数据
     */
    ApiResponse<ImageCaptchaVO> generateSliderCaptcha();

    /**
     * 校验滑块验证码轨迹。
     *
     * @param request 校验请求
     * @return 二次校验结果
     */
    ApiResponse<CaptchaVerificationResponse> checkSliderCaptcha(CaptchaCheckRequest request);

    /**
     * 在登录阶段消费验证码校验凭证。
     *
     * @param captchaVerificationId 验证码校验凭证
     */
    void validateLoginCaptcha(String captchaVerificationId);

    /**
     * 强制消费滑动验证码校验凭证。
     *
     * @param captchaVerificationId 验证码校验凭证
     */
    void validateRequiredCaptcha(String captchaVerificationId);
}
