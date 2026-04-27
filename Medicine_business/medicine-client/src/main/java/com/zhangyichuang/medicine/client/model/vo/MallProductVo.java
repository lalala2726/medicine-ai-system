package com.zhangyichuang.medicine.client.model.vo;

import com.zhangyichuang.medicine.model.dto.DrugDetailDto;
import com.zhangyichuang.medicine.model.vo.MallProductTagVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品详情 VO（客户端）。
 *
 * @author Chuang
 * created on 2025/10/17
 */
@Data
public class MallProductVo {

    /**
     * 商品ID。
     */
    @Schema(description = "商品ID", example = "1")
    private Long id;

    /**
     * 商品名称。
     */
    @Schema(description = "商品名称", example = "商品名称")
    private String name;

    /**
     * 商品分类ID列表。
     */
    @Schema(description = "商品分类ID列表", example = "[1,2]")
    private List<Long> categoryIds;

    /**
     * 商品分类名称列表。
     */
    @Schema(description = "商品分类名称列表", example = "[\"保健品\",\"维生素\"]")
    private List<String> categoryNames;

    /**
     * 商品单位（件、盒、瓶等）。
     */
    @Schema(description = "商品单位（件、盒、瓶等）", example = "件")
    private String unit;

    /**
     * 展示价/兜底价。
     */
    @Schema(description = "展示价/兜底价：单规格=唯一SKU价，多规格=最小SKU价；结算以SKU价为准", example = "10.00")
    private BigDecimal price;

    /**
     * 库存。
     */
    @Schema(description = "库存", example = "100")
    private Integer stock;

    /**
     * 是否允许使用优惠券。
     */
    @Schema(description = "是否允许使用优惠券（1-允许，0-不允许）", example = "1")
    private Integer couponEnabled;

    /**
     * 商品图片列表。
     */
    @Schema(description = "商品图片列表", example = "[\"https://example.com/image1.jpg\", \"https://example.com/image2.jpg\"]")
    private List<String> images;

    /**
     * 药品说明信息。
     */
    @Schema(description = "药品说明信息")
    private DrugDetailDto drugDetail;

    /**
     * 商品标签列表。
     */
    @Schema(description = "商品标签列表")
    private List<MallProductTagVo> tags;

    /**
     * 销量（已完成订单数量）。
     */
    @Schema(description = "销量（已完成订单数量）", example = "256")
    private Integer sales;
}
