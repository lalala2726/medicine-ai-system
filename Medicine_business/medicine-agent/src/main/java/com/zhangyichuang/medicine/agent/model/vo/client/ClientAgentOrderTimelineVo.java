package com.zhangyichuang.medicine.agent.model.vo.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.zhangyichuang.medicine.agent.annotation.AgentCodeLabel;
import com.zhangyichuang.medicine.agent.annotation.FieldDescription;
import com.zhangyichuang.medicine.agent.mapping.AgentCodeLabelRegistry;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 客户端智能体订单时间线。
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "客户端智能体订单时间线")
@FieldDescription(description = "客户端智能体订单时间线")
public class ClientAgentOrderTimelineVo {

    /**
     * 订单ID。
     */
    @Schema(description = "订单ID")
    @FieldDescription(description = "订单ID")
    private Long orderId;

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
     * 时间线节点。
     */
    @Schema(description = "时间线节点")
    @FieldDescription(description = "时间线节点")
    private List<TimelineNode> timeline;

    /**
     * 订单时间线节点。
     */
    @Data
    @Schema(description = "订单时间线节点")
    @FieldDescription(description = "订单时间线节点")
    public static class TimelineNode {

        /**
         * 时间线ID。
         */
        @Schema(description = "时间线ID")
        @FieldDescription(description = "时间线ID")
        private Long id;

        /**
         * 事件类型编码。
         */
        @Schema(description = "事件类型编码")
        @FieldDescription(description = "事件类型编码")
        @AgentCodeLabel(dictKey = AgentCodeLabelRegistry.AGENT_ORDER_EVENT_TYPE)
        private String eventType;

        /**
         * 事件类型名称。
         */
        @Schema(description = "事件类型名称")
        @FieldDescription(description = "事件类型名称")
        private String eventTypeName;

        /**
         * 事件状态编码。
         */
        @Schema(description = "事件状态编码")
        @FieldDescription(description = "事件状态编码")
        @AgentCodeLabel(dictKey = AgentCodeLabelRegistry.AGENT_ORDER_STATUS)
        private String eventStatus;

        /**
         * 事件状态名称。
         */
        @Schema(description = "事件状态名称")
        @FieldDescription(description = "事件状态名称")
        private String eventStatusName;

        /**
         * 操作方类型编码。
         */
        @Schema(description = "操作方类型编码")
        @FieldDescription(description = "操作方类型编码")
        @AgentCodeLabel(dictKey = AgentCodeLabelRegistry.AGENT_OPERATOR_TYPE)
        private String operatorType;

        /**
         * 操作方类型名称。
         */
        @Schema(description = "操作方类型名称")
        @FieldDescription(description = "操作方类型名称")
        private String operatorTypeName;

        /**
         * 节点描述。
         */
        @Schema(description = "节点描述")
        @FieldDescription(description = "节点描述")
        private String description;

        /**
         * 节点时间。
         */
        @Schema(description = "节点时间")
        @FieldDescription(description = "节点时间")
        private Date createdTime;
    }
}
