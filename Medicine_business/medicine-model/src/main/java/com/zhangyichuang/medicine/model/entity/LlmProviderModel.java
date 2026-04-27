package com.zhangyichuang.medicine.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 大模型提供商模型配置表
 */
@TableName(value = "llm_provider_model")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LlmProviderModel {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 提供商ID，对应 llm_provider.id
     */
    private Long providerId;

    /**
     * 模型实际名称，例如 qwen-max、text-embedding-v3
     */
    private String modelName;

    /**
     * 模型类型：CHAT/EMBEDDING/RERANK
     */
    private String modelType;

    /**
     * 是否支持深度思考：0否 1是
     */
    private Integer supportReasoning;

    /**
     * 是否支持图片识别：0否 1是
     */
    private Integer supportVision;

    /**
     * 模型描述
     */
    private String description;

    /**
     * 状态：0启用 1停用
     */
    private Integer enabled;

    /**
     * 排序值，值越小越靠前
     */
    private Integer sort;

    /**
     * 创建人账号
     */
    private String createBy;

    /**
     * 最后更新人账号
     */
    private String updateBy;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 最后更新时间
     */
    private Date updatedAt;
}
