package com.zhangyichuang.medicine.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 用户钱包视图对象
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "用户钱包视图对象")
public class UserWalletVo {
    @Schema(description = "用户ID", example = "1")
    private Long userId;

    @Schema(description = "钱包编号", example = "W2022010100000000000000000001")
    private String walletNo;

    @Schema(description = "可用余额", example = "100.00")
    private BigDecimal balance;

    @Schema(description = "累计入账金额", example = "100.00")
    private BigDecimal totalIncome;

    @Schema(description = "累计支出金额", example = "100.00")
    private BigDecimal totalExpend;

    @Schema(description = "币种", example = "CNY")
    private String currency;

    @Schema(description = "状态", example = "0")
    private Integer status;

    @Schema(description = "冻结原因", example = "冻结原因")
    private String freezeReason;

    @Schema(description = "冻结时间", example = "2022-01-01 00:00:00")
    private Date freezeTime;

    @Schema(description = "更新时间", example = "2022-01-01 00:00:00")
    private Date updatedAt;
}
