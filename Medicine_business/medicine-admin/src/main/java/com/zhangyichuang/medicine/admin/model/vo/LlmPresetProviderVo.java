package com.zhangyichuang.medicine.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 预设大模型厂商摘要。
 */
@Data
@Schema(description = "预设大模型厂商摘要")
public class LlmPresetProviderVo {

    @Schema(description = "预设厂商英文键", example = "aliyun-bailian")
    private String providerKey;

    @Schema(description = "提供商名称", example = "阿里云百联")
    private String providerName;

    @Schema(description = "提供商类型，仅支持 aliyun", example = "aliyun")
    private String providerType;

    @Schema(description = "基础请求地址", example = "https://dashscope.aliyuncs.com/compatible-mode/v1")
    private String baseUrl;

    @Schema(description = "描述", example = "阿里云百联 OpenAI 兼容接口预设模板")
    private String description;
}
