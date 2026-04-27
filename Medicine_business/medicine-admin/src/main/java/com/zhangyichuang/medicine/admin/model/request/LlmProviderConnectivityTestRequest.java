package com.zhangyichuang.medicine.admin.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 大模型提供商连通性测试请求。
 */
@Data
@Schema(description = "大模型提供商连通性测试请求")
public class LlmProviderConnectivityTestRequest {

    @Schema(description = "OpenAI 兼容接口基础地址", example = "https://api.openai.com/v1")
    @NotBlank(message = "基础地址不能为空")
    private String baseUrl;

    @Schema(description = "API Key", example = "sk-xxxx")
    @NotBlank(message = "API Key不能为空")
    private String apiKey;
}
