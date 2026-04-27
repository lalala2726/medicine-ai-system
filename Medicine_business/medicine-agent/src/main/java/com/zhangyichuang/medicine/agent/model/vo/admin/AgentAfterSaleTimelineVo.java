package com.zhangyichuang.medicine.agent.model.vo.admin;

import com.zhangyichuang.medicine.agent.annotation.AgentCodeLabel;
import com.zhangyichuang.medicine.agent.annotation.FieldDescription;
import com.zhangyichuang.medicine.agent.mapping.AgentCodeLabelRegistry;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 管理端智能体售后时间线视图。
 */
@Schema(description = "管理端智能体售后时间线")
@FieldDescription(description = "管理端智能体售后时间线")
@Data
public class AgentAfterSaleTimelineVo {

    @Schema(description = "时间线ID", example = "1")
    @FieldDescription(description = "时间线ID")
    private Long id;

    @Schema(description = "事件类型编码")
    @FieldDescription(description = "事件类型编码")
    @AgentCodeLabel(dictKey = AgentCodeLabelRegistry.AGENT_ORDER_EVENT_TYPE)
    private String eventType;

    @Schema(description = "事件类型名称", example = "退款申请")
    @FieldDescription(description = "事件类型名称")
    private String eventTypeName;

    @Schema(description = "事件状态编码")
    @FieldDescription(description = "事件状态编码")
    @AgentCodeLabel(dictKey = AgentCodeLabelRegistry.AGENT_AFTER_SALE_STATUS)
    private String eventStatus;

    @Schema(description = "操作人类型编码")
    @FieldDescription(description = "操作人类型编码")
    @AgentCodeLabel(dictKey = AgentCodeLabelRegistry.AGENT_OPERATOR_TYPE)
    private String operatorType;

    @Schema(description = "操作人类型名称", example = "客户")
    @FieldDescription(description = "操作人类型名称")
    private String operatorTypeName;

    @Schema(description = "事件描述", example = "用户申请退款")
    @FieldDescription(description = "事件描述")
    private String description;

    @Schema(description = "创建时间", example = "2025-11-08 10:00:00")
    @FieldDescription(description = "创建时间")
    private Date createTime;
}
