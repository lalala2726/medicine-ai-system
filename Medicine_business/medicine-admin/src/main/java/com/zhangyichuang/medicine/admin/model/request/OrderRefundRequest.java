package com.zhangyichuang.medicine.admin.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @author Chuang
 * <p>
 * created on 2025/11/1
 */
@Data
@Schema(description = "订单退款请求参数")
public class OrderRefundRequest {

    @Schema(description = "订单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "202511010001")
    @NotBlank(message = "订单编号不能为空")
    private String orderNo;

    @Schema(description = "退款金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.00")
    @NotBlank(message = "退款金额不能为空")
    private BigDecimal refundAmount;

    @Schema(description = "退款原因", requiredMode = Schema.RequiredMode.REQUIRED, example = "用户取消订单")
    private String refundReason;
}
