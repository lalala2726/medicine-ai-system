package com.zhangyichuang.medicine.admin.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * 大模型提供商 API Key 修改请求。
 */
@Data
@Schema(description = "大模型提供商 API Key 修改请求")
public class LlmProviderApiKeyUpdateRequest {

    @Schema(description = "提供商ID", example = "1")
    @NotNull(message = "提供商ID不能为空")
    @Positive(message = "提供商ID必须大于0")
    private Long id;

    @Schema(description = "新的 API Key", example = "sk-xxxx")
    @NotBlank(message = "API Key不能为空")
    private String apiKey;
}
