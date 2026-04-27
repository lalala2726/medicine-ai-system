package com.zhangyichuang.medicine.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 商品标签类型新增请求。
 *
 * @author Chuang
 */
@Data
@Schema(description = "商品标签类型新增请求")
public class MallProductTagTypeAddRequest {

    /**
     * 标签类型编码。
     */
    @NotBlank(message = "标签类型编码不能为空")
    @Schema(description = "标签类型编码", example = "CROWD", requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;

    /**
     * 标签类型名称。
     */
    @NotBlank(message = "标签类型名称不能为空")
    @Schema(description = "标签类型名称", example = "适用人群", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    /**
     * 排序值。
     */
    @Schema(description = "排序值，越小越靠前", example = "1")
    private Integer sort = 0;

    /**
     * 状态。
     */
    @NotNull(message = "标签类型状态不能为空")
    @Schema(description = "状态：1-启用，0-禁用", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status = 1;
}
