package com.zhangyichuang.medicine.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Agent 模型下拉选项视图对象。
 */
@Data
@Schema(description = "Agent模型下拉选项视图对象")
public class AgentModelOptionVo {

    @Schema(description = "选项标签", example = "gpt-4.1")
    private String label;

    @Schema(description = "选项值", example = "gpt-4.1")
    private String value;

    @Schema(description = "当前模型是否支持深度思考", example = "true")
    private Boolean supportReasoning;

    @Schema(description = "当前模型是否支持图片理解", example = "false")
    private Boolean supportVision;

    @Schema(description = "模型描述", example = "适用于日常通用型任务，综合能力均衡")
    private String description;
}
