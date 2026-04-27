package com.zhangyichuang.medicine.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 客户端智能体订单时间线 DTO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "客户端智能体订单时间线")
public class ClientAgentOrderTimelineDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 订单ID。
     */
    @Schema(description = "订单ID")
    private Long orderId;

    /**
     * 订单编号。
     */
    @Schema(description = "订单编号")
    private String orderNo;

    /**
     * 当前订单状态编码。
     */
    @Schema(description = "当前订单状态编码")
    private String orderStatus;

    /**
     * 当前订单状态名称。
     */
    @Schema(description = "当前订单状态名称")
    private String orderStatusName;

    /**
     * 时间线节点。
     */
    @Schema(description = "时间线节点")
    private List<TimelineNode> timeline;

    /**
     * 订单时间线节点。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "订单时间线节点")
    public static class TimelineNode implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 时间线ID。
         */
        @Schema(description = "时间线ID")
        private Long id;

        /**
         * 事件类型编码。
         */
        @Schema(description = "事件类型编码")
        private String eventType;

        /**
         * 事件类型名称。
         */
        @Schema(description = "事件类型名称")
        private String eventTypeName;

        /**
         * 事件状态编码。
         */
        @Schema(description = "事件状态编码")
        private String eventStatus;

        /**
         * 事件状态名称。
         */
        @Schema(description = "事件状态名称")
        private String eventStatusName;

        /**
         * 操作方类型编码。
         */
        @Schema(description = "操作方类型编码")
        private String operatorType;

        /**
         * 操作方类型名称。
         */
        @Schema(description = "操作方类型名称")
        private String operatorTypeName;

        /**
         * 节点描述。
         */
        @Schema(description = "节点描述")
        private String description;

        /**
         * 节点创建时间。
         */
        @Schema(description = "节点创建时间")
        private Date createdTime;
    }
}
