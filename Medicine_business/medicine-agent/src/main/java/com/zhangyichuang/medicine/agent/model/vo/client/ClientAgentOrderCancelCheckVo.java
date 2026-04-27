package com.zhangyichuang.medicine.agent.model.vo.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.zhangyichuang.medicine.agent.annotation.AgentCodeLabel;
import com.zhangyichuang.medicine.agent.annotation.FieldDescription;
import com.zhangyichuang.medicine.agent.mapping.AgentCodeLabelRegistry;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 客户端智能体订单取消资格校验。
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "客户端智能体订单取消资格校验")
@FieldDescription(description = "客户端智能体订单取消资格校验")
public class ClientAgentOrderCancelCheckVo {

    /**
     * 订单编号。
     */
    @Schema(description = "订单编号")
    @FieldDescription(description = "订单编号")
    private String orderNo;

    /**
     * 当前订单状态编码。
     */
    @Schema(description = "当前订单状态编码")
    @FieldDescription(description = "当前订单状态编码")
    @AgentCodeLabel(dictKey = AgentCodeLabelRegistry.AGENT_ORDER_STATUS)
    private String orderStatus;

    /**
     * 当前订单状态名称。
     */
    @Schema(description = "当前订单状态名称")
    @FieldDescription(description = "当前订单状态名称")
    private String orderStatusName;

    /**
     * 是否允许取消。
     */
    @Schema(description = "是否允许取消")
    @FieldDescription(description = "是否允许取消")
    private Boolean cancelable;

    /**
     * 结果编码。
     */
    @Schema(description = "结果编码")
    @FieldDescription(description = "结果编码")
    private String reasonCode;

    /**
     * 结果说明。
     */
    @Schema(description = "结果说明")
    @FieldDescription(description = "结果说明")
    private String reasonMessage;
}
