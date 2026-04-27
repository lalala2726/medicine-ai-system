package com.zhangyichuang.medicine.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 豆包语音 Agent 配置视图对象。
 * <p>
 * 语音识别与语音合成的 ResourceId 为后端固定值，详情接口不回显。
 */
@Data
@Schema(description = "豆包语音Agent配置视图对象")
public class SpeechAgentConfigVo {

    @Schema(description = "火山引擎语音应用ID")
    private String appId;

    @Schema(description = "火山引擎语音访问令牌，不回显时返回 null")
    private String accessToken;

    @Schema(description = "语音合成配置")
    private TextToSpeechConfigVo textToSpeech;
}
