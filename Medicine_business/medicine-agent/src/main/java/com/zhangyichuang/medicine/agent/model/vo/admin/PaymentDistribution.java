package com.zhangyichuang.medicine.agent.model.vo.admin;

import com.zhangyichuang.medicine.agent.annotation.AgentCodeLabel;
import com.zhangyichuang.medicine.agent.annotation.FieldDescription;
import com.zhangyichuang.medicine.agent.mapping.AgentCodeLabelRegistry;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 支付方式分布。
 */
@Data
@Schema(description = "支付方式分布")
@FieldDescription(description = "支付方式分布")
public class PaymentDistribution {

    @Schema(description = "支付方式")
    @FieldDescription(description = "支付方式")
    @AgentCodeLabel(dictKey = AgentCodeLabelRegistry.AGENT_ORDER_PAY_TYPE)
    private String payType;

    @Schema(description = "支付方式名称")
    @FieldDescription(description = "支付方式名称")
    private String payTypeName;

    @Schema(description = "订单数量")
    @FieldDescription(description = "订单数量")
    private Long count;

    @Schema(description = "金额合计")
    @FieldDescription(description = "金额合计")
    private BigDecimal amount;
}
