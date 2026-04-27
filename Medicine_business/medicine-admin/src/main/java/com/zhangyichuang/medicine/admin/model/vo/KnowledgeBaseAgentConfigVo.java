package com.zhangyichuang.medicine.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 知识库 Agent 配置视图对象。
 */
@Data
@Schema(description = "知识库Agent配置视图对象")
public class KnowledgeBaseAgentConfigVo {

    @Schema(description = "是否启用知识库", example = "true")
    private Boolean enabled;

    @Schema(description = "可访问知识库名称列表")
    private List<String> knowledgeNames;

    @Schema(description = "向量维度", example = "1024")
    private Integer embeddingDim;

    @Schema(description = "知识库检索默认返回条数", example = "10")
    private Integer topK;

    @Schema(description = "向量模型槽位配置")
    private AgentModelSelectionVo embeddingModel;

    @Schema(description = "是否启用排序", example = "false")
    private Boolean rankingEnabled;

    @Schema(description = "排序模型槽位配置，可为空表示未配置")
    private AgentModelSelectionVo rankingModel;
}
