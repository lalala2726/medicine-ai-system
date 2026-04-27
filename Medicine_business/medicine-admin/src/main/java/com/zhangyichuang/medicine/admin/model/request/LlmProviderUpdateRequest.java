package com.zhangyichuang.medicine.admin.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

/**
 * 编辑大模型提供商请求。
 */
@Data
@Schema(description = "编辑大模型提供商请求")
public class LlmProviderUpdateRequest {

    @Schema(description = "主键ID", example = "1")
    @NotNull(message = "提供商ID不能为空")
    @Positive(message = "提供商ID必须大于0")
    private Long id;

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

    @Schema(description = "提供商描述", example = "阿里云百联 OpenAI 兼容接口")
    private String description;

    @Schema(description = "排序值，值越小越靠前", example = "10")
    private Integer sort;

    @Schema(description = "模型列表，编辑时按整组替换处理")
    @NotEmpty(message = "模型列表不能为空")
    private List<@NotNull(message = "模型信息不能为空") @Valid LlmProviderModelItemRequest> models;
}
