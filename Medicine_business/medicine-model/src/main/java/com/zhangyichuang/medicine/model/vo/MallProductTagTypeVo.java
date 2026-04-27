package com.zhangyichuang.medicine.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 商品标签类型简要视图对象。
 *
 * @author Chuang
 */
@Data
@Schema(description = "商品标签类型简要视图对象")
public class MallProductTagTypeVo {

    /**
     * 标签类型ID。
     */
    @Schema(description = "标签类型ID", example = "1")
    private Long id;

    /**
     * 标签类型编码。
     */
    @Schema(description = "标签类型编码", example = "CROWD")
    private String code;

    /**
     * 标签类型名称。
     */
    @Schema(description = "标签类型名称", example = "适用人群")
    private String name;
}
