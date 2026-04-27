package com.zhangyichuang.medicine.admin.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 提供商模型项请求。
 */
@Data
@Schema(description = "提供商模型项请求")
public class LlmProviderModelItemRequest {

    @Schema(description = "模型实际名称", example = "gpt-4.1")
    @NotBlank(message = "模型名称不能为空")
    private String modelName;

    @Schema(description = "模型类型：CHAT/EMBEDDING/RERANK", example = "CHAT")
    @NotBlank(message = "模型类型不能为空")
    private String modelType;

    @Schema(description = "是否支持深度思考（0否 1是）", example = "0")
    @Min(value = 0L, message = "是否支持深度思考值不合法")
    @Max(value = 1L, message = "是否支持深度思考值不合法")
    private Integer supportReasoning;

    @Schema(description = "是否支持图片识别（0否 1是）", example = "0")
    @Min(value = 0L, message = "是否支持图片识别值不合法")
    @Max(value = 1L, message = "是否支持图片识别值不合法")
    private Integer supportVision;

    @Schema(description = "模型描述", example = "通用对话模型")
    private String description;

    @Schema(description = "状态（0启用 1停用）", example = "0")
    @Min(value = 0L, message = "模型状态值不合法")
    @Max(value = 1L, message = "模型状态值不合法")
    private Integer enabled;

    @Schema(description = "排序值，值越小越靠前", example = "10")
    private Integer sort;
}
