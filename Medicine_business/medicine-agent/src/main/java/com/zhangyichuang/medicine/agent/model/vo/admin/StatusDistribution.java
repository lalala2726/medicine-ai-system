package com.zhangyichuang.medicine.agent.model.vo.admin;

import com.zhangyichuang.medicine.agent.annotation.AgentCodeLabel;
import com.zhangyichuang.medicine.agent.annotation.FieldDescription;
import com.zhangyichuang.medicine.agent.mapping.AgentCodeLabelRegistry;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 订单状态分布。
 */
@Data
@Schema(description = "订单状态分布")
@FieldDescription(description = "订单状态分布")
public class StatusDistribution {

    @Schema(description = "订单状态")
    @FieldDescription(description = "订单状态")
    @AgentCodeLabel(dictKey = AgentCodeLabelRegistry.AGENT_ORDER_STATUS)
    private String status;

    @Schema(description = "订单状态名称")
    @FieldDescription(description = "订单状态名称")
    private String statusName;

    @Schema(description = "数量")
    @FieldDescription(description = "数量")
    private Long count;
}
