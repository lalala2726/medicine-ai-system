package com.zhangyichuang.medicine.client.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 订单取消请求参数
 *
 * @author Chuang
 * created on 2025/11/13
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "订单取消请求参数")
public class OrderCancelRequest {

    @NotBlank(message = "订单编号不能为空")
    @Schema(description = "订单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "O20251113112233445566")
    private String orderNo;

    @NotBlank(message = "取消原因不能为空")
    @Schema(description = "取消原因", requiredMode = Schema.RequiredMode.REQUIRED, example = "不想要了")
    private String cancelReason;
}

