package com.zhangyichuang.medicine.client.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 用户端售后资格校验结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户端售后资格校验结果")
public class AfterSaleEligibilityVo {

    @Schema(description = "订单编号")
    private String orderNo;

    @Schema(description = "请求范围")
    private String requestedScope;

    @Schema(description = "最终生效范围")
    private String resolvedScope;

    @Schema(description = "是否可申请售后")
    private Boolean eligible;

    @Schema(description = "结果编码")
    private String reasonCode;

    @Schema(description = "结果说明")
    private String reasonMessage;

    @Schema(description = "订单状态编码")
    private String orderStatus;

    @Schema(description = "订单状态名称")
    private String orderStatusName;

    @Schema(description = "选中的订单项ID")
    private Long selectedOrderItemId;

    @Schema(description = "选中范围可退金额")
    private BigDecimal selectedRefundableAmount;

    @Schema(description = "整单可退金额")
    private BigDecimal totalRefundableAmount;

    @Schema(description = "售后截止时间")
    private Date afterSaleDeadlineTime;

    @Schema(description = "订单商品售后校验列表")
    private List<ItemEligibility> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "订单商品售后校验项")
    public static class ItemEligibility {

        @Schema(description = "订单项ID")
        private Long orderItemId;

        @Schema(description = "商品ID")
        private Long productId;

        @Schema(description = "商品名称")
        private String productName;

        @Schema(description = "商品图片")
        private String imageUrl;

        @Schema(description = "购买数量")
        private Integer quantity;

        @Schema(description = "商品单价")
        private BigDecimal price;

        @Schema(description = "商品小计金额")
        private BigDecimal totalPrice;

        @Schema(description = "已退款金额")
        private BigDecimal refundedAmount;

        @Schema(description = "当前可退金额")
        private BigDecimal refundableAmount;

        @Schema(description = "售后状态")
        private String afterSaleStatus;

        @Schema(description = "该商品是否可申请售后")
        private Boolean eligible;

        @Schema(description = "结果编码")
        private String reasonCode;

        @Schema(description = "结果说明")
        private String reasonMessage;
    }
}
