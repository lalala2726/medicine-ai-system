package com.zhangyichuang.medicine.admin.model.request;

import com.zhangyichuang.medicine.common.core.base.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 大模型提供商分页查询参数。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "大模型提供商分页查询参数")
public class LlmProviderListRequest extends PageRequest {

    @Schema(description = "提供商名称（模糊）", example = "OpenAI")
    private String providerName;

    @Schema(description = "状态（精确，1启用 0停用）", example = "1")
    private Integer status;
}
