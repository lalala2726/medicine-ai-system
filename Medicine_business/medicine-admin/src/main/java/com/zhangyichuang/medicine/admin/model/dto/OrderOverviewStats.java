package com.zhangyichuang.medicine.admin.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 聚合订单概况数据，避免重复的多次数据库查询。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderOverviewStats {


    /**
     * 订单总量
     */
    private long totalOrders;

    /**
     * 待支付数量
     */
    private long pendingPayment;

    /**
     * 待发货数量
     */
    private long pendingShipment;

    /**
     * 待收货数量
     */
    private long pendingReceipt;

    /**
     * 已完成数量
     */
    private long completed;

    /**
     * 已退款数量（订单维度）
     */
    private long refunded;

    /**
     * 售后中数量
     */
    private long afterSale;

    /**
     * 已取消数量
     */
    private long cancelled;

    /**
     * 已支付金额合计（排除取消/已退款订单）
     */
    private BigDecimal totalSales;

    /**
     * 退款金额合计
     */
    private BigDecimal refundedAmount;
}
