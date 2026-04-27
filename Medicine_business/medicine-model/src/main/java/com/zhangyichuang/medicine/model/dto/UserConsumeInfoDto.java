package com.zhangyichuang.medicine.model.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 用户消费信息 DTO。
 */
@Data
public class UserConsumeInfoDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 流水索引
     */
    private Long index;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 订单编号
     */
    private String orderNo;

    /**
     * 商品总价
     */
    private BigDecimal totalPrice;

    /**
     * 实付金额
     */
    private BigDecimal payPrice;

    /**
     * 完成时间
     */
    private Date finishTime;
}
