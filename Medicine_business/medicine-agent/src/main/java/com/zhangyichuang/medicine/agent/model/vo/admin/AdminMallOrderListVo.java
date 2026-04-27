package com.zhangyichuang.medicine.agent.model.vo.admin;

import com.zhangyichuang.medicine.agent.annotation.AgentCodeLabel;
import com.zhangyichuang.medicine.agent.annotation.FieldDescription;
import com.zhangyichuang.medicine.agent.mapping.AgentCodeLabelRegistry;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 管理端智能体订单列表视图。
 */
@Schema(description = "管理端智能体订单列表")
@FieldDescription(description = "管理端智能体订单列表")
@Data
public class AdminMallOrderListVo {

    @Schema(description = "订单ID", example = "1")
    @FieldDescription(description = "订单ID")
    private Long id;

    @Schema(description = "订单编号（业务唯一标识）", example = "O202510312122")
    @FieldDescription(description = "订单编号（业务唯一标识）")
    private String orderNo;

    @Schema(description = "订单总金额（含运费）", example = "100.00")
    @FieldDescription(description = "订单总金额（含运费）")
    private BigDecimal totalAmount;

    @Schema(description = "支付方式")
    @FieldDescription(description = "支付方式")
    @AgentCodeLabel(dictKey = AgentCodeLabelRegistry.AGENT_ORDER_PAY_TYPE)
    private String payType;

    @Schema(description = "订单状态")
    @FieldDescription(description = "订单状态")
    @AgentCodeLabel(dictKey = AgentCodeLabelRegistry.AGENT_ORDER_STATUS)
    private String orderStatus;

    @Schema(description = "支付时间", example = "2025-10-31 21:22:00")
    @FieldDescription(description = "支付时间")
    private Date payTime;

    @Schema(description = "创建时间", example = "2025-10-31 21:22:00")
    @FieldDescription(description = "创建时间")
    private Date createTime;

    @Schema(description = "商品信息")
    @FieldDescription(description = "商品信息")
    private MallOrderProductInfoVo productInfo;
}
