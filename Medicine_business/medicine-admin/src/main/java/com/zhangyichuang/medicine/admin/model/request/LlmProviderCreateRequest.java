package com.zhangyichuang.medicine.admin.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

/**
 * 新增大模型提供商请求。
 */
@Data
@Schema(description = "新增大模型提供商请求")
public class LlmProviderCreateRequest {

    @Schema(description = "预设厂商英文键，仅用于前端模板标识", example = "aliyun-bailian")
    private String providerKey;

    @Schema(description = "提供商名称", example = "阿里云百联")
    @NotBlank(message = "提供商名称不能为空")
    private String providerName;

    @Schema(description = "提供商类型，仅支持 aliyun", example = "aliyun")
    @NotBlank(message = "提供商类型不能为空")
    @Pattern(regexp = "^aliyun$", message = "提供商类型不合法")
    private String providerType;

    @Schema(description = "基础请求地址", example = "https://dashscope.aliyuncs.com/compatible-mode/v1")
    @NotBlank(message = "基础地址不能为空")
    private String baseUrl;

    @Schema(description = "API Key", example = "sk-xxxx")
    @NotBlank(message = "API Key不能为空")
    private String apiKey;

    @Schema(description = "提供商描述", example = "阿里云百联 OpenAI 兼容接口")
    private String description;

    @Schema(description = "排序值，值越小越靠前", example = "10")
    private Integer sort;

    @Schema(description = "模型列表")
    @NotEmpty(message = "模型列表不能为空")
    private List<@NotNull(message = "模型信息不能为空") @Valid LlmProviderModelItemRequest> models;
}
