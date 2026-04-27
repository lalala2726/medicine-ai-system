package com.zhangyichuang.medicine.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 钱包流水记录表
 */
@TableName(value = "user_wallet_log")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserWalletLog {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联钱包ID
     */
    private Long walletId;

    /**
     * 用户ID（冗余存储，便于查询）
     */
    private Long userId;

    /**
     * 流水编号，例如 WALFLOW202510310001
     */
    private String flowNo;

    /**
     * 原因说明
     */
    private String reason;

    /**
     * 业务关联单号（如订单号、提现单号等）
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
     * 备注说明（如：支付订单#1001）
     */
    private String remark;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 逻辑删除(0否,1是)
     */
    @TableLogic
    private Integer isDeleted;
}
