package com.zhangyichuang.medicine.client.model.vo;

import com.zhangyichuang.medicine.model.enums.OrderItemAfterSaleStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 *
 * @author Chuang
 * created 2025/11/10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "订单列表")
public class AssistantOrderListVo {

    @Schema(description = "订单ID", example = "1234567890")
    private Long id;

    @Schema(description = "订单状态", example = "PENDING")
    private String orderStatus;

    @Schema(description = "订单状态名称", example = "待支付")
    private String orderStatusName;

    @Schema(description = "订单总金额", example = "299.99")
    private BigDecimal totalAmount;

    @Schema(description = "是否存在售后", example = "NO_AFTER_SALE")
    private OrderItemAfterSaleStatusEnum afterSaleFlag;

    @Schema(description = "创建时间", example = "2025-01-10T10:30:00")
    private Date createTime;

    @Schema(description = "订单商品列表(只包含必要信息)")
    private List<OrderItemSimpleVo> items;

    /**
     * 订单项简化信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "订单项简化信息")
    public static class OrderItemSimpleVo {

        @Schema(description = "订单项ID", example = "9876543210")
        private Long id;

        @Schema(description = "商品ID", example = "5001")
        private Long productId;

        @Schema(description = "商品名称", example = "999感冒灵颗粒")
        private String productName;

        @Schema(description = "商品图片", example = "https://example.com/images/product/999.jpg")
        private String imageUrl;

        @Schema(description = "购买数量", example = "2")
        private Integer quantity;

        @Schema(description = "单价", example = "25.50")
        private BigDecimal price;

        @Schema(description = "小计金额", example = "51.00")
        private BigDecimal totalPrice;

        @Schema(description = "售后状态", example = "NO_AFTER_SALE")
        private String afterSaleStatus;
    }
}

