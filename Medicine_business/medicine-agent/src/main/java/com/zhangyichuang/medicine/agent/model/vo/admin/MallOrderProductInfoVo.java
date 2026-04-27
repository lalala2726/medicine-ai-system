package com.zhangyichuang.medicine.agent.model.vo.admin;

import com.zhangyichuang.medicine.agent.annotation.FieldDescription;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 管理端智能体订单商品信息视图。
 */
@Schema(description = "管理端智能体订单商品信息")
@FieldDescription(description = "管理端智能体订单商品信息")
@Data
public class MallOrderProductInfoVo {

    @Schema(description = "商品名称", example = "商品名称")
    @FieldDescription(description = "商品名称")
    private String productName;

    @Schema(description = "商品图片", example = "商品图片")
    @FieldDescription(description = "商品图片")
    private String productImage;

    @Schema(description = "商品价格", example = "100.00")
    @FieldDescription(description = "商品价格")
    private BigDecimal productPrice;

    @Schema(description = "商品分类")
    @FieldDescription(description = "商品分类")
    private String productCategory;

    @Schema(description = "商品ID", example = "1")
    @FieldDescription(description = "商品ID")
    private Long productId;

    @Schema(description = "商品数量", example = "1")
    @FieldDescription(description = "商品数量")
    private Integer quantity;
}
