package com.zhangyichuang.medicine.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 预设大模型厂商模型视图对象。
 */
@Data
@Schema(description = "预设大模型厂商模型视图对象")
public class LlmPresetProviderModelVo {

    @Schema(description = "模型实际名称", example = "gpt-4.1")
    private String modelName;

    @Schema(description = "模型类型", example = "CHAT")
    private String modelType;

    @Schema(description = "是否支持深度思考（0否 1是）", example = "0")
    private Integer supportReasoning;

    @Schema(description = "是否支持图片识别（0否 1是）", example = "0")
    private Integer supportVision;

    @Schema(description = "模型描述", example = "OpenAI 通用对话模型")
    private String description;

    @Schema(description = "状态（0启用 1停用）", example = "0")
    private Integer enabled;

    @Schema(description = "排序值", example = "10")
    private Integer sort;
}
