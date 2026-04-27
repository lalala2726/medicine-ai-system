package com.zhangyichuang.medicine.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 大模型提供商详情视图对象。
 */
@Data
@Schema(description = "大模型提供商详情视图对象")
public class LlmProviderDetailVo {

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

    @Schema(description = "创建人")
    private String createBy;

    @Schema(description = "更新人")
    private String updateBy;

    @Schema(description = "创建时间")
    private Date createdAt;

    @Schema(description = "更新时间")
    private Date updatedAt;

    @Schema(description = "模型列表")
    private List<LlmProviderModelVo> models;
}
