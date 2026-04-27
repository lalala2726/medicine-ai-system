package com.zhangyichuang.medicine.admin.model.vo;

import com.zhangyichuang.medicine.model.dto.DrugDetailDto;
import com.zhangyichuang.medicine.model.vo.MallProductTagVo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 商城商品详情视图对象。
 *
 * @author Chuang
 * created on 2025/10/4
 */
@Data
@Schema(description = "商城商品详情视图对象")
public class MallProductVo {

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
     * 商品单位。
     */
    @Schema(description = "商品单位", example = "盒")
    private String unit;

    /**
     * 基础售价。
     */
    @Schema(description = "基础售价", example = "29.90")
    private BigDecimal price;

    /**
     * 商品库存数量。
     */
    @Schema(description = "商品库存数量", example = "50")
    private Integer stock;

    /**
     * 排序值。
     */
    @Schema(description = "排序值，越小越靠前", example = "1")
    private Integer sort;

    /**
     * 状态（1-上架，0-下架）。
     */
    @Schema(description = "状态（1-上架，0-下架）", example = "1")
    private Integer status;

    /**
     * 配送方式。
     */
    @Schema(description = "配送方式", example = "快递")
    private Integer deliveryType;

    /**
     * 是否允许使用优惠券。
     */
    @Schema(description = "是否允许使用优惠券（1-允许，0-不允许）", example = "1")
    private Integer couponEnabled;

    /**
     * 创建时间。
     */
    @Schema(description = "创建时间", example = "2025-01-01 00:00:00")
    private Date createTime;

    /**
     * 更新时间。
     */
    @Schema(description = "更新时间", example = "2025-01-01 00:00:00")
    private Date updateTime;

    /**
     * 创建者。
     */
    @Schema(description = "创建者", example = "admin")
    private String createBy;

    /**
     * 更新者。
     */
    @Schema(description = "更新者", example = "admin")
    private String updateBy;

    /**
     * 商品图片列表。
     */
    @Schema(description = "商品图片列表", example = "[\"https://example.com/image1.jpg\", \"https://example.com/image2.jpg\"]")
    private List<String> images;

    /**
     * 商品标签列表。
     */
    @Schema(description = "商品标签列表")
    private List<MallProductTagVo> tags;

    /**
     * 药品说明信息。
     */
    @Schema(description = "药品说明信息")
    @NotNull(message = "药品说明信息不能为空")
    private DrugDetailDto drugDetail;
}
