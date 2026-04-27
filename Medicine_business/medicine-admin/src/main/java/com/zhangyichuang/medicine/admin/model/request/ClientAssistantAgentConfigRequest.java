package com.zhangyichuang.medicine.admin.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 客户端助手 Agent 配置请求对象。
 */
@Data
@Schema(description = "客户端助手Agent配置请求对象")
public class ClientAssistantAgentConfigRequest {

    @Schema(description = "路由模型槽位配置")
    @Valid
    @NotNull(message = "路由模型槽位配置不能为空")
    private ClientAssistantModelSelectionRequest routeModel;

    @Schema(description = "服务节点模型槽位配置")
    @Valid
    @NotNull(message = "服务节点模型槽位配置不能为空")
    private ClientAssistantModelSelectionRequest serviceNodeModel;

    @Schema(description = "诊断节点模型槽位配置")
    @Valid
    @NotNull(message = "诊断节点模型槽位配置不能为空")
    private ClientAssistantModelSelectionRequest diagnosisNodeModel;

    @Schema(description = "是否允许客户端聊天开启统一深度思考", example = "false")
    @NotNull(message = "统一深度思考开关不能为空")
    private Boolean reasoningEnabled;
}
