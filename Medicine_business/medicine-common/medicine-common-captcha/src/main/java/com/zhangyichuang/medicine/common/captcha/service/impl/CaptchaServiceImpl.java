package com.zhangyichuang.medicine.common.captcha.service.impl;

import cloud.tianai.captcha.application.ImageCaptchaApplication;
import cloud.tianai.captcha.application.vo.ImageCaptchaVO;
import cloud.tianai.captcha.common.constant.CaptchaTypeConstant;
import cloud.tianai.captcha.common.response.ApiResponse;
import com.zhangyichuang.medicine.common.captcha.config.CaptchaProperties;
import com.zhangyichuang.medicine.common.captcha.model.CaptchaCheckRequest;
import com.zhangyichuang.medicine.common.captcha.model.CaptchaVerificationResponse;
import com.zhangyichuang.medicine.common.captcha.service.CaptchaService;
import com.zhangyichuang.medicine.common.core.enums.ResponseCode;
import com.zhangyichuang.medicine.common.core.exception.LoginException;
import com.zhangyichuang.medicine.common.core.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

/**
 * 验证码服务实现。
 *
 * @author Chuang
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaptchaServiceImpl implements CaptchaService {

    /**
     * 登录校验凭证键后缀。
     */
    private static final String VERIFICATION_KEY_SUFFIX = ":verification:";

    /**
     * 滑块验证码类型。
     */
    private static final String CAPTCHA_TYPE_SLIDER = CaptchaTypeConstant.SLIDER;

    /**
     * 验证码功能关闭提示语。
     */
    private static final String CAPTCHA_DISABLED_MESSAGE = "验证码功能未开启";

    /**
     * 验证码请求参数缺失提示语。
     */
    private static final String CAPTCHA_REQUEST_REQUIRED_MESSAGE = "验证码请求参数不能为空";

    /**
     * 验证码轨迹参数缺失提示语。
     */
    private static final String CAPTCHA_TRACK_REQUIRED_MESSAGE = "验证码轨迹不能为空";

    /**
     * 登录前置验证码缺失提示语。
     */
    private static final String CAPTCHA_VALIDATION_REQUIRED_MESSAGE = "请先完成滑动验证码校验";

    /**
     * 登录前置验证码失效提示语。
     */
    private static final String CAPTCHA_VALIDATION_INVALID_MESSAGE = "验证码校验已失效，请重新验证";

    /**
     * 验证码校验结果保存失败提示语。
     */
    private static final String CAPTCHA_STORE_FAILED_MESSAGE = "验证码校验结果保存失败";

    /**
     * 验证码应用实例。
     */
    private final ImageCaptchaApplication imageCaptchaApplication;

    /**
     * 验证码配置。
     */
    private final CaptchaProperties captchaProperties;

    /**
     * 项目统一 RedisTemplate。
     */
    private final RedisTemplate<Object, Object> redisTemplate;

    /**
     * 生成滑块验证码。
     *
     * @return 滑块验证码数据
     */
    @Override
    public ApiResponse<ImageCaptchaVO> generateSliderCaptcha() {
        if (!captchaProperties.isEnabled()) {
            return ApiResponse.ofError(CAPTCHA_DISABLED_MESSAGE);
        }
        return imageCaptchaApplication.generateCaptcha(CAPTCHA_TYPE_SLIDER);
    }

    /**
     * 校验滑块验证码。
     *
     * @param request 校验请求
     * @return 二次校验结果
     */
    @Override
    public ApiResponse<CaptchaVerificationResponse> checkSliderCaptcha(CaptchaCheckRequest request) {
        if (!captchaProperties.isEnabled()) {
            return ApiResponse.ofError(CAPTCHA_DISABLED_MESSAGE);
        }
        if (request == null || !StringUtils.hasText(request.getId())) {
            return ApiResponse.ofCheckError(CAPTCHA_REQUEST_REQUIRED_MESSAGE);
        }
        if (request.getData() == null) {
            return ApiResponse.ofCheckError(CAPTCHA_TRACK_REQUIRED_MESSAGE);
        }
        String captchaId = request.getId().trim();
        ApiResponse<?> matchingResponse = imageCaptchaApplication.matching(captchaId, request.getData());
        if (!matchingResponse.isSuccess()) {
            return matchingResponse.convert();
        }
        try {
            storeVerificationResult(captchaId);
        } catch (Exception exception) {
            log.error("保存验证码二次校验结果失败, captchaId={}", captchaId, exception);
            return ApiResponse.ofError(CAPTCHA_STORE_FAILED_MESSAGE);
        }
        return ApiResponse.ofSuccess(new CaptchaVerificationResponse(captchaId));
    }

    /**
     * 校验并消费登录验证码。
     *
     * @param captchaVerificationId 验证码校验凭证
     */
    @Override
    public void validateLoginCaptcha(String captchaVerificationId) {
        if (!captchaProperties.isEnabled() || !captchaProperties.isLoginRequired()) {
            return;
        }
        validateCaptchaVerification(captchaVerificationId, true);
    }

    /**
     * 强制校验并消费滑动验证码。
     *
     * @param captchaVerificationId 验证码校验凭证
     */
    @Override
    public void validateRequiredCaptcha(String captchaVerificationId) {
        if (!captchaProperties.isEnabled()) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, CAPTCHA_DISABLED_MESSAGE);
        }
        validateCaptchaVerification(captchaVerificationId, false);
    }

    /**
     * 校验并消费验证码校验凭证。
     *
     * @param captchaVerificationId 验证码校验凭证
     * @param loginScene            是否登录场景
     */
    private void validateCaptchaVerification(String captchaVerificationId, boolean loginScene) {
        if (!StringUtils.hasText(captchaVerificationId)) {
            throw buildCaptchaException(loginScene, CAPTCHA_VALIDATION_REQUIRED_MESSAGE);
        }
        Object verificationResult = redisTemplate.opsForValue().getAndDelete(buildVerificationKey(captchaVerificationId.trim()));
        if (verificationResult == null) {
            throw buildCaptchaException(loginScene, CAPTCHA_VALIDATION_INVALID_MESSAGE);
        }
    }

    /**
     * 创建验证码校验异常。
     *
     * @param loginScene 是否登录场景
     * @param message    错误提示
     * @return 验证码校验异常
     */
    private RuntimeException buildCaptchaException(boolean loginScene, String message) {
        if (loginScene) {
            return new LoginException(ResponseCode.LOGIN_ERROR, message);
        }
        return new ServiceException(ResponseCode.PARAM_ERROR, message);
    }

    /**
     * 保存登录阶段消费的验证码凭证。
     *
     * @param captchaVerificationId 验证码校验凭证
     */
    private void storeVerificationResult(String captchaVerificationId) {
        redisTemplate.opsForValue().set(buildVerificationKey(captchaVerificationId),
                Boolean.TRUE,
                captchaProperties.getVerificationExpireMs(),
                TimeUnit.MILLISECONDS);
    }

    /**
     * 构建验证码校验凭证 Redis 键。
     *
     * @param captchaVerificationId 验证码校验凭证
     * @return Redis 键
     */
    private String buildVerificationKey(String captchaVerificationId) {
        return captchaProperties.getRedisKeyPrefix() + VERIFICATION_KEY_SUFFIX + captchaVerificationId;
    }
}
