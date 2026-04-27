package com.zhangyichuang.medicine.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 钱包流水记录创建参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserWalletLogRecordDto {

    /**
     * 钱包ID
     */
    private Long walletId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 流水编号（可选）
     */
    private String flowNo;

    /**
     * 原因
     */
    private String reason;

    /**
     * 业务关联ID
     */
    private String bizId;

    /**
     * 变动类型：1收入、2支出、3冻结、4解冻
     */
    private Integer changeType;

    /**
     * 变动金额
     */
    private BigDecimal amount;

    /**
     * 变动前余额
     */
    private BigDecimal beforeBalance;

    /**
     * 变动后余额
     */
    private BigDecimal afterBalance;

    /**
     * 备注
     */
    private String remark;
}
