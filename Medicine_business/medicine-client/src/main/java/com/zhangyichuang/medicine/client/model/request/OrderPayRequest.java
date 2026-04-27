package com.zhangyichuang.medicine.client.model.request;

import com.zhangyichuang.medicine.model.enums.PayTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 订单支付请求参数
 * <p>
 * 用于对已创建的待支付订单进行支付操作
 * </p>
 *
 * @author Chuang
 * created on 2025/11/13
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "订单支付请求参数")
public class OrderPayRequest {

    @NotBlank(message = "订单编号不能为空")
    @Schema(description = "订单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "O20251113112233445566")
    private String orderNo;

    @NotNull(message = "支付方式不能为空")
    @Schema(description = "支付方式", requiredMode = Schema.RequiredMode.REQUIRED, example = "WALLET")
    private PayTypeEnum payMethod;
}

