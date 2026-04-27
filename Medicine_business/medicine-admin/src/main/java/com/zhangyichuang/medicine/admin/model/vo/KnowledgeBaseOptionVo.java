package com.zhangyichuang.medicine.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 知识库下拉选项视图对象。
 */
@Data
@Schema(description = "知识库下拉选项视图对象")
public class KnowledgeBaseOptionVo {

    @Schema(description = "知识库唯一名称（业务键）", example = "common_medicine_kb")
    private String knowledgeName;

    @Schema(description = "知识库展示名称", example = "常见用药知识库")
    private String displayName;

    @Schema(description = "知识库向量模型", example = "text-embedding-3-large")
    private String embeddingModel;

    @Schema(description = "知识库向量维度", example = "1024")
    private Integer embeddingDim;
}
