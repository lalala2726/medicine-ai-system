package com.zhangyichuang.medicine.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 商品标签修改请求。
 *
 * @author Chuang
 */
@Data
@Schema(description = "商品标签修改请求")
public class MallProductTagUpdateRequest {

    /**
     * 标签ID。
     */
    @NotNull(message = "标签ID不能为空")
    @Schema(description = "标签ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    /**
     * 标签名称。
     */
    @NotBlank(message = "标签名称不能为空")
    @Schema(description = "标签名称", example = "退烧", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    /**
     * 标签类型ID。
     */
    @NotNull(message = "标签类型不能为空")
    @Schema(description = "标签类型ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long typeId;

    /**
     * 排序值。
     */
    @Schema(description = "排序值，越小越靠前", example = "1")
    private Integer sort = 0;
}
