package com.zhangyichuang.medicine.client.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 商品搜索标签筛选分组视图对象。
 *
 * @author Chuang
 */
@Data
@Schema(description = "商品搜索标签筛选分组视图对象")
public class MallProductSearchTagFilterVo {

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

    /**
     * 当前类型下的筛选项列表。
     */
    @Schema(description = "当前类型下的筛选项列表")
    private List<MallProductSearchTagFilterOptionVo> options;
}
