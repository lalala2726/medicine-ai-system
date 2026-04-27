package com.zhangyichuang.medicine.client.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 用户钱包流水详情信息。
 */
@Data
@Schema(description = "用户钱包流水详情信息")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserWalletBillDetailVo {

    /**
     * 流水主键ID。
     */
    @Schema(description = "流水主键ID", example = "1001")
    private Long id;

    /**
     * 流水编号。
     */
    @Schema(description = "流水编号", example = "WALFLOW202510310001")
    private String flowNo;

    /**
     * 流水标题。
     */
    @Schema(description = "流水标题", example = "订单支付")
    private String title;

    /**
     * 业务关联单号。
     */
    @Schema(description = "业务关联单号", example = "ORDER202510310001")
    private String bizId;

    /**
     * 变动类型。
     */
    @Schema(description = "变动类型：1收入、2支出、3冻结、4解冻", example = "2")
    private Integer changeType;

    /**
     * 变动金额。
     */
    @Schema(description = "变动金额", example = "100.00")
    private BigDecimal amount;

    /**
     * 变动前余额。
     */
    @Schema(description = "变动前余额", example = "300.00")
    private BigDecimal beforeBalance;

    /**
     * 变动后余额。
     */
    @Schema(description = "变动后余额", example = "200.00")
    private BigDecimal afterBalance;

    /**
     * 备注说明。
     */
    @Schema(description = "备注说明", example = "支付订单#1001")
    private String remark;

    /**
     * 流水时间。
     */
    @Schema(description = "流水时间", example = "2025-11-06 06:30:17")
    private Date time;
}
