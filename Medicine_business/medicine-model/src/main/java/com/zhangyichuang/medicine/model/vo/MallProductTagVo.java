package com.zhangyichuang.medicine.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 商品标签简要视图对象。
 *
 * @author Chuang
 */
@Data
@Schema(description = "商品标签简要视图对象")
public class MallProductTagVo {

    /**
     * 标签ID。
     */
    @Schema(description = "标签ID", example = "1")
    private Long id;

    /**
     * 标签名称。
     */
    @Schema(description = "标签名称", example = "退烧")
    private String name;

    /**
     * 标签类型ID。
     */
    @Schema(description = "标签类型ID", example = "1")
    private Long typeId;

    /**
     * 标签类型编码。
     */
    @Schema(description = "标签类型编码", example = "EFFICACY")
    private String typeCode;

    /**
     * 标签类型名称。
     */
    @Schema(description = "标签类型名称", example = "功效")
    private String typeName;
}
