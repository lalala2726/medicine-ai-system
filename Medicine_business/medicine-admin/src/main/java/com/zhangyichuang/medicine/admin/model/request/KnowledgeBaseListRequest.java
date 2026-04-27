package com.zhangyichuang.medicine.admin.model.request;

import com.zhangyichuang.medicine.common.core.base.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识库查询请求对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "查询知识库请求对象")
public class KnowledgeBaseListRequest extends PageRequest {

    @Schema(description = "知识库唯一名称（模糊）", example = "medicine")
    private String knowledgeName;

    @Schema(description = "知识库展示名称（模糊）", example = "常见用药知识库")
    private String displayName;

    @Schema(description = "知识库描述（模糊）", example = "覆盖常见用药相关问答内容")
    private String description;

    @Schema(description = "向量模型标识（模糊）", example = "text-embedding")
    private String embeddingModel;

    @Schema(description = "向量维度（精确）", example = "1024")
    private Integer embeddingDim;

    @Schema(description = "状态（精确，0启用 1停用）", example = "0")
    private Integer status;

}
