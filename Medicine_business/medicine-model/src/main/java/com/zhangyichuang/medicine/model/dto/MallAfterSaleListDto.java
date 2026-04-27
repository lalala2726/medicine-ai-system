package com.zhangyichuang.medicine.model.dto;

import com.zhangyichuang.medicine.model.entity.MallAfterSale;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 售后列表查询结果 DTO。
 * <p>
 * 功能描述：承接售后列表多表查询结果，在继承 {@link MallAfterSale} 主表字段基础上，
 * 补充联表展示字段，避免在实体类中添加非持久化属性。
 * </p>
 *
 * @author Chuang
 * created 2026/02/28
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "售后列表查询结果")
public class MallAfterSaleListDto extends MallAfterSale {

    /**
     * 用户昵称（来自 user 表）
     */
    @Schema(description = "用户昵称", example = "张三")
    private String userNickname;

    /**
     * 商品名称（来自 mall_order_item 表）
     */
    @Schema(description = "商品名称", example = "感冒药")
    private String productName;

    /**
     * 商品图片（来自 mall_order_item 表）
     */
    @Schema(description = "商品图片", example = "https://example.com/image.jpg")
    private String productImage;
}

