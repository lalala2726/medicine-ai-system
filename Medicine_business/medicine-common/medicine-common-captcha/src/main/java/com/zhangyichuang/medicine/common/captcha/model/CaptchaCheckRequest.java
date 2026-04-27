package com.zhangyichuang.medicine.common.captcha.model;

import cloud.tianai.captcha.validator.common.model.dto.ImageCaptchaTrack;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 验证码校验请求。
 *
 * @author Chuang
 */
@Data
@Schema(description = "滑块验证码校验请求")
public class CaptchaCheckRequest {

    /**
     * 验证码 challenge ID。
     */
    @Schema(description = "验证码 challenge ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String id;

    /**
     * 前端采集的滑动轨迹数据。
     */
    @Schema(description = "前端采集的滑动轨迹数据", requiredMode = Schema.RequiredMode.REQUIRED)
    private ImageCaptchaTrack data;
}
