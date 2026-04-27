package com.zhangyichuang.medicine.model.request;

import com.zhangyichuang.medicine.common.core.base.PageRequest;
import com.zhangyichuang.medicine.model.dto.MallProductTagFilterGroup;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商城商品列表查询请求对象。
 *
 * @author Chuang
 * created on 2025/10/4
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "商城商品列表查询请求对象")
public class MallProductListQueryRequest extends PageRequest {

    /**
     * 商品ID。
     */
    @Schema(description = "商品ID", example = "1")
    private Long id;

    /**
     * 商品名称。
     */
    @Schema(description = "商品名称", example = "维生素C片")
    private String name;

    /**
     * 商品分类ID，关联 mall_category。
     */
    @Schema(description = "商品分类ID", example = "1")
    private Long categoryId;

    /**
     * 商品标签ID列表。
     */
    @Schema(description = "商品标签ID列表", example = "[1, 2, 3]")
    private List<Long> tagIds;

    /**
     * 状态（1-上架，0-下架）。
     */
    @Schema(description = "状态（1-上架，0-下架）", example = "1")
    private Integer status;

    /**
     * 最低价格。
     */
    @Schema(description = "最低价格", example = "10.00")
    private BigDecimal minPrice;

    /**
     * 最高价格。
     */
    @Schema(description = "最高价格", example = "100.00")
    private BigDecimal maxPrice;

    /**
     * 按标签类型拆分后的筛选条件。
     * <p>
     * 该字段仅供服务层内部根据 tagIds 自动推导使用，
     * 不作为外部搜索接口的直接入参暴露。
     */
    @Schema(hidden = true)
    private List<MallProductTagFilterGroup> tagFilterGroups;
}
