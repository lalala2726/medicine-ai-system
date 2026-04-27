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
 * 大模型提供商配置表
 */
@TableName(value = "llm_provider")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LlmProvider {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 提供商显示名称
     */
    private String providerName;

    /**
     * 提供商类型：aliyun
     */
    private String providerType;

    /**
     * 基础请求地址
     */
    private String baseUrl;

    /**
     * 加密后的API Key密文
     */
    private String apiKey;

    /**
     * 提供商描述
     */
    private String description;

    /**
     * 状态：1启用 0停用
     */
    private Integer status;

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
