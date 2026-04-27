package com.zhangyichuang.medicine.admin.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 客户端助手模型选择请求对象。
 * <p>
 * 客户端助手系统配置阶段仅绑定模型，不在该请求中维护深度思考开关。
 */
@Data
@Schema(description = "客户端助手模型选择请求对象")
public class ClientAssistantModelSelectionRequest {

    @Schema(description = "模型名称", example = "gpt-4.1")
    @NotBlank(message = "模型名称不能为空")
    private String modelName;
}
