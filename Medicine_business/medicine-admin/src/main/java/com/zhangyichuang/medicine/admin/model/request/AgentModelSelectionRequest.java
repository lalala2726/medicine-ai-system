package com.zhangyichuang.medicine.admin.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Agent 模型选择请求对象。
 */
@Data
@Schema(description = "Agent模型选择请求对象")
public class AgentModelSelectionRequest {

    @Schema(description = "模型名称", example = "gpt-4.1")
    @NotBlank(message = "模型名称不能为空")
    private String modelName;

    @Schema(description = "是否开启深度思考", example = "true")
    @NotNull(message = "是否开启深度思考不能为空")
    private Boolean reasoningEnabled;

}
