package com.zhangyichuang.medicine.admin.model.dto;

import com.zhangyichuang.medicine.model.entity.LlmProviderModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * 大模型提供商详情数据。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmProviderDetailDto {

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
     * API Key。
     */
    private String apiKey;

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
     * 模型列表。
     */
    private List<LlmProviderModel> models;
}
