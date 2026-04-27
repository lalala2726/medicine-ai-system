package com.zhangyichuang.medicine.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 大模型提供商列表视图对象。
 */
@Data
@Schema(description = "大模型提供商列表视图对象")
public class LlmProviderListVo {

    @Schema(description = "主键ID", example = "1")
    private Long id;

    @Schema(description = "提供商名称", example = "阿里云百联")
    private String providerName;

    @Schema(description = "提供商类型，仅支持 aliyun", example = "aliyun")
    private String providerType;

    @Schema(description = "基础请求地址", example = "https://dashscope.aliyuncs.com/compatible-mode/v1")
    private String baseUrl;

    @Schema(description = "描述", example = "阿里云百联 OpenAI 兼容接口")
    private String description;

    @Schema(description = "状态（1启用 0停用）", example = "1")
    private Integer status;

    @Schema(description = "排序值", example = "10")
    private Integer sort;

    @Schema(description = "模型总数", example = "5")
    private Integer modelCount;

    @Schema(description = "对话模型数量", example = "3")
    private Integer chatModelCount;

    @Schema(description = "向量模型数量", example = "2")
    private Integer embeddingModelCount;

    @Schema(description = "重排模型数量", example = "1")
    private Integer rerankModelCount;

    @Schema(description = "创建时间")
    private Date createdAt;

    @Schema(description = "更新时间")
    private Date updatedAt;
}
