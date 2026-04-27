package com.zhangyichuang.medicine.client.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 查询订单支付信息请求。
 */
@Data
@Schema(description = "订单支付信息请求")
public class OrderPayInfoRequest {

    @NotBlank(message = "订单号不能为空")
    @Schema(description = "订单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "O20251113112233445566")
    private String orderNo;
}
