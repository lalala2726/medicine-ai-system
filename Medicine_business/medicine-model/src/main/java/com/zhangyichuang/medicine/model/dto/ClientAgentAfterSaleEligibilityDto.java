package com.zhangyichuang.medicine.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 客户端智能体售后资格校验 DTO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "客户端智能体售后资格校验")
public class ClientAgentAfterSaleEligibilityDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 订单编号。
     */
    @Schema(description = "订单编号")
    private String orderNo;

    /**
     * 订单项ID。
     */
    @Schema(description = "订单项ID")
    private Long orderItemId;

    /**
     * 校验范围。
     */
    @Schema(description = "校验范围，ORDER-整单，ITEM-订单项")
    private String scope;

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
     * 是否满足售后资格。
     */
    @Schema(description = "是否满足售后资格")
    private Boolean eligible;

    /**
     * 结果编码。
     */
    @Schema(description = "结果编码")
    private String reasonCode;

    /**
     * 结果说明。
     */
    @Schema(description = "结果说明")
    private String reasonMessage;

    /**
     * 可退款金额。
     */
    @Schema(description = "可退款金额")
    private BigDecimal refundableAmount;
}
