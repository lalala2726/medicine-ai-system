package com.zhangyichuang.medicine.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 客户端助手 Agent 配置视图对象。
 */
@Data
@Schema(description = "客户端助手Agent配置视图对象")
public class ClientAssistantAgentConfigVo {

    @Schema(description = "路由模型槽位配置")
    private AgentModelSelectionVo routeModel;

    @Schema(description = "服务节点模型槽位配置")
    private AgentModelSelectionVo serviceNodeModel;

    @Schema(description = "诊断节点模型槽位配置")
    private AgentModelSelectionVo diagnosisNodeModel;

    @Schema(description = "是否允许客户端聊天开启统一深度思考", example = "false")
    private Boolean reasoningEnabled;
}
