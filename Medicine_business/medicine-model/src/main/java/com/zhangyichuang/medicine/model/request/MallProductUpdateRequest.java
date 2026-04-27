package com.zhangyichuang.medicine.model.request;

import com.zhangyichuang.medicine.model.dto.DrugDetailDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商城商品修改请求对象。
 *
 * @author Chuang
 * created on 2025/10/4
 */
@Data
@Schema(description = "商城商品修改请求对象")
public class MallProductUpdateRequest {

    /**
     * 商品ID。
     */
    @NotNull(message = "商品ID不能为空")
    @Schema(description = "商品ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    /**
     * 商品名称。
     */
    @NotBlank(message = "商品名称不能为空")
    @Schema(description = "商品名称", example = "维生素C片", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    /**
     * 商品分类ID列表。
     */
    @NotEmpty(message = "商品分类不能为空")
    @Schema(description = "商品分类ID列表", example = "[1, 2]", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> categoryIds;

    /**
     * 商品单位。
     */
    @NotBlank(message = "商品单位不能为空")
    @Schema(description = "商品单位", example = "盒", requiredMode = Schema.RequiredMode.REQUIRED)
    private String unit;

    /**
     * 商品售价。
     */
    @NotNull(message = "商品价格不能为空")
    @Schema(description = "基础售价", example = "29.90", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal price;

    /**
     * 商品库存。
     */
    @Schema(description = "商品库存数量", example = "50")
    private Integer stock;

    /**
     * 排序值。
     */
    @Schema(description = "排序值，越小越靠前", example = "1")
    private Integer sort;

    /**
     * 上下架状态。
     */
    @Schema(description = "状态（1-上架，0-下架）", example = "1")
    private Integer status;

    /**
     * 配送方式。
     */
    @NotNull(message = "配送方式不能为空")
    @Schema(description = "配送方式（快递、自提、同城配送等）", example = "1")
    private Integer deliveryType;

    /**
     * 是否允许使用优惠券。
     */
    @Schema(description = "是否允许使用优惠券（1-允许，0-不允许）", example = "1")
    @NotNull(message = "是否允许使用优惠券不能为空")
    private Integer couponEnabled;

    /**
     * 运费模板ID。
     */
    @Schema(description = "运费模板ID", example = "1")
    private Long shippingId;

    /**
     * 商品图片列表。
     */
    @NotEmpty(message = "商品图片列表不能为空")
    @Schema(description = "商品图片列表", example = "[\"https://example.com/image1.jpg\", \"https://example.com/image2.jpg\"]")
    private List<String> images;

    /**
     * 绑定的标签ID集合。
     */
    @Schema(description = "商品标签ID集合", example = "[1, 2, 3]")
    private List<Long> tagIds;

    /**
     * 药品说明信息。
     */
    @Schema(description = "商品详情")
    private DrugDetailDto drugDetail;
}
