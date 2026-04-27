package com.zhangyichuang.medicine.agent.model.vo.admin;

import com.zhangyichuang.medicine.agent.annotation.AgentCodeLabel;
import com.zhangyichuang.medicine.agent.annotation.FieldDescription;
import com.zhangyichuang.medicine.agent.mapping.AgentCodeLabelRegistry;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 管理端智能体商品列表视图。
 */
@Schema(description = "管理端智能体商品列表视图")
@FieldDescription(description = "管理端智能体商品列表视图")
@Data
public class AgentProductListVo {

    @Schema(description = "商品ID", example = "1")
    @FieldDescription(description = "商品ID")
    private Long id;

    @Schema(description = "商品名称", example = "维生素C片")
    @FieldDescription(description = "商品名称")
    private String name;

    @Schema(description = "商品分类ID", example = "1")
    @FieldDescription(description = "商品分类ID")
    private Long categoryId;

    @Schema(description = "商品分类名称", example = "保健品")
    @FieldDescription(description = "商品分类名称")
    private String categoryName;

    @Schema(description = "商品单位", example = "盒")
    @FieldDescription(description = "商品单位")
    private String unit;

    @Schema(description = "基础售价", example = "29.90")
    @FieldDescription(description = "基础售价")
    private BigDecimal price;

    @Schema(description = "商品库存数量", example = "50")
    @FieldDescription(description = "商品库存数量")
    private Integer stock;

    @Schema(description = "商品销量", example = "10")
    @FieldDescription(description = "商品销量")
    private Integer sales;

    @Schema(description = "排序值，越小越靠前", example = "1")
    @FieldDescription(description = "排序值，越小越靠前")
    private Integer sort;

    @Schema(description = "状态", example = "1")
    @FieldDescription(description = "状态")
    @AgentCodeLabel(dictKey = AgentCodeLabelRegistry.AGENT_PRODUCT_STATUS)
    private Integer status;

    @Schema(description = "配送方式", example = "1")
    @FieldDescription(description = "配送方式")
    @AgentCodeLabel(dictKey = AgentCodeLabelRegistry.AGENT_PRODUCT_DELIVERY_TYPE)
    private Integer deliveryType;

    @Schema(description = "创建时间", example = "2025-01-01 00:00:00")
    @FieldDescription(description = "创建时间")
    private Date createTime;

    @Schema(description = "更新时间", example = "2025-01-01 00:00:00")
    @FieldDescription(description = "更新时间")
    private Date updateTime;

    @Schema(description = "创建者", example = "admin")
    @FieldDescription(description = "创建者")
    private String createBy;

    @Schema(description = "更新者", example = "admin")
    @FieldDescription(description = "更新者")
    private String updateBy;

    @Schema(description = "商品展示图", example = "https://example.com/image1.jpg")
    @FieldDescription(description = "商品展示图")
    private String coverImage;
}
