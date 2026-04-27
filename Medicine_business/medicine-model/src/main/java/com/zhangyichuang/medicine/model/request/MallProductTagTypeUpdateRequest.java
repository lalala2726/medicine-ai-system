package com.zhangyichuang.medicine.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 商品标签类型修改请求。
 *
 * @author Chuang
 */
@Data
@Schema(description = "商品标签类型修改请求")
public class MallProductTagTypeUpdateRequest {

    /**
     * 标签类型ID。
     */
    @NotNull(message = "标签类型ID不能为空")
    @Schema(description = "标签类型ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

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
}
