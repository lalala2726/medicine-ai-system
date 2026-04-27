package com.zhangyichuang.medicine.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 语音合成配置视图对象。
 */
@Data
@Schema(description = "语音合成配置视图对象")
public class TextToSpeechConfigVo {

    @Schema(description = "火山引擎语音合成音色类型")
    private String voiceType;

    @Schema(description = "单次语音合成最大文本长度")
    private Integer maxTextChars;
}
