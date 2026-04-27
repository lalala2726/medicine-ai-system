package com.zhangyichuang.medicine.admin.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 语音合成配置请求对象。
 */
@Data
@Schema(description = "语音合成配置请求对象")
public class TextToSpeechConfigRequest {

    @Schema(description = "火山引擎语音合成音色类型")
    @NotBlank(message = "语音合成VoiceType不能为空")
    private String voiceType;

    @Schema(description = "单次语音合成最大文本长度")
    @NotNull(message = "语音合成最大文本长度不能为空")
    @Min(value = 1, message = "语音合成最大文本长度不能小于1")
    @Max(value = 3000, message = "语音合成最大文本长度不能大于3000")
    private Integer maxTextChars;
}
