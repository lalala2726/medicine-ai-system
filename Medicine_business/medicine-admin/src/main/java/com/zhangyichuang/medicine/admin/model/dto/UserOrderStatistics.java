package com.zhangyichuang.medicine.admin.model.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UserOrderStatistics {

    /**
     * 订单总数
     */
    private Long totalOrderCount;

    /**
     * 订单总金额
     */
    private BigDecimal totalConsumption;
}
