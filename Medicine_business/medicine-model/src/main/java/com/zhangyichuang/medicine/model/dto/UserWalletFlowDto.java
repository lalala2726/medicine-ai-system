package com.zhangyichuang.medicine.model.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 用户钱包流水 DTO。
 */
@Data
public class UserWalletFlowDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 流水索引
     */
    private Long index;

    /**
     * 变动类型（如：订单支付、充值、退款等）
     */
    private String changeType;

    /**
     * 变动金额
     */
    private BigDecimal amount;

    /**
     * 金额变动方向（1-收入，2-支出，3-冻结，4-解冻）
     */
    private Integer amountDirection;

    /**
     * 是否为收入
     */
    private Boolean isIncome;

    /**
     * 变动前余额
     */
    private BigDecimal beforeBalance;

    /**
     * 变动后余额
     */
    private BigDecimal afterBalance;

    /**
     * 变动时间
     */
    private Date changeTime;
}
