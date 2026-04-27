package com.zhangyichuang.medicine.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 客户端智能体订单取消资格校验 DTO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "客户端智能体订单取消资格校验")
public class ClientAgentOrderCancelCheckDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

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
     * 是否允许取消。
     */
    @Schema(description = "是否允许取消")
    private Boolean cancelable;

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
}
