package com.zhangyichuang.medicine.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 客户端智能体售后资格校验请求。
 */
@Data
@Schema(description = "客户端智能体售后资格校验请求")
public class ClientAgentAfterSaleEligibilityRequest implements Serializable {

    /**
     * 序列化版本号。
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 订单编号。
     */
    @NotBlank(message = "订单编号不能为空")
    @Schema(description = "订单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "O202511130001")
    private String orderNo;

    /**
     * 订单项ID。
     */
    @Min(value = 1, message = "订单项ID不能小于1")
    @Schema(description = "订单项ID，不传表示校验整单售后资格", example = "1001")
    private Long orderItemId;
}
