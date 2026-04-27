package com.zhangyichuang.medicine.agent.model.vo.admin;

import com.zhangyichuang.medicine.agent.annotation.AgentCodeLabel;
import com.zhangyichuang.medicine.agent.annotation.FieldDescription;
import com.zhangyichuang.medicine.agent.mapping.AgentCodeLabelRegistry;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 管理端用户钱包流水信息。
 */
@Data
@Schema(description = "管理端用户钱包流水信息")
@FieldDescription(description = "管理端用户钱包流水信息")
public class UserWalletFlowVo {

    @Schema(description = "流水索引")
    @FieldDescription(description = "流水索引")
    private Long index;

    @Schema(description = "变动类型")
    @FieldDescription(description = "变动类型")
    @AgentCodeLabel(source = "amountDirection", dictKey = AgentCodeLabelRegistry.AGENT_USER_WALLET_CHANGE_TYPE)
    private String changeType;

    @Schema(description = "变动金额")
    @FieldDescription(description = "变动金额")
    private BigDecimal amount;

    @Schema(description = "金额变动方向")
    @FieldDescription(description = "金额变动方向")
    @AgentCodeLabel(dictKey = AgentCodeLabelRegistry.AGENT_USER_WALLET_CHANGE_TYPE)
    private Integer amountDirection;

    @Schema(description = "是否为收入")
    @FieldDescription(description = "是否为收入")
    private Boolean isIncome;

    @Schema(description = "变动前余额")
    @FieldDescription(description = "变动前余额")
    private BigDecimal beforeBalance;

    @Schema(description = "变动后余额")
    @FieldDescription(description = "变动后余额")
    private BigDecimal afterBalance;

    @Schema(description = "变动时间")
    @FieldDescription(description = "变动时间")
    private Date changeTime;
}
