package com.zhangyichuang.medicine.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 用户钱包表
 */
@TableName(value = "user_wallet")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserWallet {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID，对应系统用户表ID
     */
    private Long userId;

    /**
     * 钱包编号，唯一标识一个用户的钱包
     */
    private String walletNo;

    /**
     * 可用余额
     */
    private BigDecimal balance;

    /**
     * 冻结金额（提现中或仲裁中）
     */
    private BigDecimal frozenBalance;

    /**
     * 累计入账金额（充值、退款等）
     */
    private BigDecimal totalIncome;

    /**
     * 累计支出金额（消费、提现等）
     */
    private BigDecimal totalExpend;

    /**
     * 币种，默认人民币
     */
    private String currency;

    /**
     * 状态：0正常，1冻结
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
     * 版本号
     */
    @Version
    private Integer version;

    /**
     * 是否删除：0否，1是
     */
    @TableLogic
    private Integer isDeleted;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 更新时间
     */
    private Date updatedAt;
}
