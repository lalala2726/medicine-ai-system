package com.zhangyichuang.medicine.admin.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 豆包语音 Agent 配置请求对象。
 * <p>
 * 语音识别与语音合成的 ResourceId 由后端固定写入，管理端无需传入。
 */
@Data
@Schema(description = "豆包语音Agent配置请求对象")
public class SpeechAgentConfigRequest {

    @Schema(description = "火山引擎语音应用ID")
    @NotBlank(message = "豆包语音AppId不能为空")
    private String appId;

    @Schema(description = "火山引擎语音访问令牌，首次保存必填，后续留空则保留原Token")
    private String accessToken;

    @Schema(description = "语音合成配置，ResourceId 由后端固定写入")
    @Valid
    @NotNull(message = "语音合成配置不能为空")
    private TextToSpeechConfigRequest textToSpeech;
}
