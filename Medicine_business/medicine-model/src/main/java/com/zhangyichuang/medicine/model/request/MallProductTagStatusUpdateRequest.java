package com.zhangyichuang.medicine.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 商品标签状态修改请求。
 *
 * @author Chuang
 */
@Data
@Schema(description = "商品标签状态修改请求")
public class MallProductTagStatusUpdateRequest {

    /**
     * 标签ID。
     */
    @NotNull(message = "标签ID不能为空")
    @Schema(description = "标签ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    /**
     * 目标状态。
     */
    @NotNull(message = "标签状态不能为空")
    @Schema(description = "状态：1-启用，0-禁用", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status;
}
