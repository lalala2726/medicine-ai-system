package com.zhangyichuang.medicine.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author Chuang
 * <p>
 * created on 2025/11/7
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "用户钱包流水信息")
public class UserWalletFlowInfoVo {

    @Schema(description = "流水索引", example = "1")
    private Long index;

    @Schema(description = "变动类型", example = "充值")
    private String changeType;

    @Schema(description = "变动金额", example = "10.00")
    private BigDecimal amount;

    @Schema(description = "金额变动方向：1收入(正)、2支出(负)、3冻结、4解冻", example = "1")
    private Integer amountDirection;

    @Schema(description = "是否为收入", example = "true")
    private Boolean isIncome;

    @Schema(description = "变动前余额", example = "10.00")
    private BigDecimal beforeBalance;

    @Schema(description = "变动后余额", example = "10.00")
    private BigDecimal afterBalance;

    @Schema(description = "变动时间", example = "2025-11-07 04:33:00")
    private Date changeTime;

}
