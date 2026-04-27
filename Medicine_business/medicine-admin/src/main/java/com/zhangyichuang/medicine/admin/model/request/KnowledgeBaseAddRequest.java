package com.zhangyichuang.medicine.admin.model.request;

import com.zhangyichuang.medicine.admin.support.KnowledgeBaseEmbeddingDimSupport;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * 添加知识库请求对象
 */
@Data
@Schema(description = "添加知识库请求对象")
public class KnowledgeBaseAddRequest {

    @Schema(description = "知识库业务名称", example = "drug_faq")
    @NotBlank(message = "知识库名称不能为空")
    private String knowledgeName;

    @Schema(description = "知识库展示名称", example = "常见用药知识库")
    @NotBlank(message = "知识库展示名称不能为空")
    private String displayName;

    @Schema(description = "知识库封面，可为空", example = "https://example.com/kb-cover.png")
    private String cover;

    @Schema(description = "知识库描述", example = "覆盖常见用药相关问答内容")
    private String description;

    @Schema(description = "向量模型标识", example = "text-embedding-3-large")
    @NotBlank(message = "向量模型标识不能为空")
    private String embeddingModel;

    @Schema(description = KnowledgeBaseEmbeddingDimSupport.SCHEMA_DESCRIPTION, example = "1024")
    @NotNull(message = "向量维度不能为空")
    private Integer embeddingDim;

    @Schema(description = "状态（0启用 1停用）", example = "0")
    @Min(value = 0L, message = "状态值不合法")
    @Max(value = 1L, message = "状态值不合法")
    private Integer status;

    @AssertTrue(message = KnowledgeBaseEmbeddingDimSupport.SUPPORTED_DIM_MESSAGE)
    public boolean isEmbeddingDimSupported() {
        return embeddingDim == null || KnowledgeBaseEmbeddingDimSupport.isSupported(embeddingDim);
    }

}
