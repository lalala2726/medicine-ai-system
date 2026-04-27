package com.zhangyichuang.medicine.client.model.vo;

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
 * created on 2025/11/6
 */
@Data
@Schema(description = "用户钱包流水信息")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserWalletBillVo {

    @Schema(description = "流水主键ID", example = "1001")
    private Long id;

    @Schema(description = "流水索引", example = "1")
    private Long index;

    @Schema(description = "流水标题", example = "充值")
    private String title;

    @Schema(description = "流水时间", example = "2025-11-06 06:30:17")
    private Date time;

    @Schema(description = "流水金额", example = "100.00")
    private BigDecimal amount;

    @Schema(description = "是否充值", example = "true")
    private Boolean isRecharge;
}
