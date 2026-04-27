package com.zhangyichuang.medicine.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 商品单位视图对象。
 *
 * @author Chuang
 */
@Data
@Schema(description = "商品单位视图对象")
public class MallProductUnitVo {

    /**
     * 单位ID。
     */
    @Schema(description = "单位ID", example = "1")
    private Long id;

    /**
     * 单位名称。
     */
    @Schema(description = "单位名称", example = "盒")
    private String name;

    /**
     * 排序值。
     */
    @Schema(description = "排序值，越小越靠前", example = "10")
    private Integer sort;
}
