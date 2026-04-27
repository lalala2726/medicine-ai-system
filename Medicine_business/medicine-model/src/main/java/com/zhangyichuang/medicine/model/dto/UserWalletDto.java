package com.zhangyichuang.medicine.model.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 用户钱包 DTO。
 */
@Data
public class UserWalletDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 钱包编号
     */
    private String walletNo;

    /**
     * 可用余额
     */
    private BigDecimal balance;

    /**
     * 累计入账金额
     */
    private BigDecimal totalIncome;

    /**
     * 累计支出金额
     */
    private BigDecimal totalExpend;

    /**
     * 币种（如：CNY）
     */
    private String currency;

    /**
     * 钱包状态（0-正常，1-冻结）
     */
    private Integer status;

    /**
     * 冻结原因
     */
    private String freezeReason;

    /**
     * 冻结时间
     */
    private Date freezeTime;

    /**
     * 更新时间
     */
    private Date updatedAt;
}
