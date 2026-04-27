package com.zhangyichuang.medicine.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 知识库详情视图对象
 */
@Data
@Schema(description = "知识库详情视图对象")
public class KnowledgeBaseVo {

    @Schema(description = "主键ID", example = "1")
    private Long id;

    @Schema(description = "知识库唯一名称（业务键）", example = "common_medicine_kb")
    private String knowledgeName;

    @Schema(description = "知识库展示名称", example = "常见用药知识库")
    private String displayName;

    @Schema(description = "知识库封面", example = "https://example.com/kb-cover.png")
    private String cover;

    @Schema(description = "知识库描述", example = "覆盖常见用药相关问答内容")
    private String description;

    @Schema(description = "向量模型标识", example = "text-embedding-3-large")
    private String embeddingModel;

    @Schema(description = "向量维度", example = "1024")
    private Integer embeddingDim;

    @Schema(description = "状态（0启用 1停用）", example = "0")
    private Integer status;

    @Schema(description = "创建时间")
    private Date createdAt;

    @Schema(description = "更新时间")
    private Date updatedAt;

    @Schema(description = "创建人")
    private String createBy;

    @Schema(description = "修改人")
    private String updateBy;
}
