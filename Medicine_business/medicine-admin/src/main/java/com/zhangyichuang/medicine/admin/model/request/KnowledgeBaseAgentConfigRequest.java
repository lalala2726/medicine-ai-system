package com.zhangyichuang.medicine.admin.model.request;

import com.zhangyichuang.medicine.admin.support.KnowledgeBaseEmbeddingDimSupport;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 知识库 Agent 配置请求对象。
 */
@Data
@Schema(description = "知识库Agent配置请求对象")
public class KnowledgeBaseAgentConfigRequest {

    @Schema(description = "是否启用知识库", example = "true")
    @NotNull(message = "是否启用知识库不能为空")
    private Boolean enabled;

    @Schema(description = "可访问知识库名称列表", example = "[\"common_medicine_kb\", \"otc_guide_kb\"]")
    @Size(max = 5, message = "知识库最多支持5个")
    private List<@NotBlank(message = "知识库名称不能为空") String> knowledgeNames;

    @Schema(description = KnowledgeBaseEmbeddingDimSupport.SCHEMA_DESCRIPTION, example = "1024")
    private Integer embeddingDim;

    @Schema(description = "向量模型槽位配置")
    @Valid
    private AgentModelSelectionRequest embeddingModel;

    @Schema(description = "知识库检索默认返回条数，为空表示使用AI端默认值", example = "10")
    private Integer topK;

    @Schema(description = "是否启用排序", example = "false")
    private Boolean rankingEnabled;

    @Schema(description = "排序模型槽位配置，可为空表示未配置")
    @Valid
    private AgentModelSelectionRequest rankingModel;

    @AssertTrue(message = "知识库名称列表不能为空")
    public boolean isKnowledgeNamesPresentWhenEnabled() {
        return !Boolean.TRUE.equals(enabled) || (knowledgeNames != null && !knowledgeNames.isEmpty());
    }

    @AssertTrue(message = "向量维度不能为空")
    public boolean isEmbeddingDimPresentWhenEnabled() {
        return !Boolean.TRUE.equals(enabled) || embeddingDim != null;
    }

    @AssertTrue(message = KnowledgeBaseEmbeddingDimSupport.SUPPORTED_DIM_MESSAGE)
    public boolean isEmbeddingDimSupported() {
        return embeddingDim == null || KnowledgeBaseEmbeddingDimSupport.isSupported(embeddingDim);
    }

    @AssertTrue(message = "向量模型槽位配置不能为空")
    public boolean isEmbeddingModelPresentWhenEnabled() {
        return !Boolean.TRUE.equals(enabled) || embeddingModel != null;
    }

    @AssertTrue(message = "是否启用排序不能为空")
    public boolean isRankingEnabledPresentWhenEnabled() {
        return !Boolean.TRUE.equals(enabled) || rankingEnabled != null;
    }
}
