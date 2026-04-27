package com.zhangyichuang.medicine.admin.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 大模型提供商列表数据。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmProviderListDto {

    /**
     * 主键ID。
     */
    private Long id;

    /**
     * 提供商名称。
     */
    private String providerName;

    /**
     * 提供商类型。
     */
    private String providerType;

    /**
     * 基础请求地址。
     */
    private String baseUrl;

    /**
     * 提供商描述。
     */
    private String description;

    /**
     * 状态（1启用 0停用）。
     */
    private Integer status;

    /**
     * 排序值。
     */
    private Integer sort;

    /**
     * 创建人。
     */
    private String createBy;

    /**
     * 更新人。
     */
    private String updateBy;

    /**
     * 创建时间。
     */
    private Date createdAt;

    /**
     * 更新时间。
     */
    private Date updatedAt;

    /**
     * 模型总数。
     */
    private Integer modelCount;

    /**
     * 对话模型数量。
     */
    private Integer chatModelCount;

    /**
     * 向量模型数量。
     */
    private Integer embeddingModelCount;

    /**
     * 重排模型数量。
     */
    private Integer rerankModelCount;
}
