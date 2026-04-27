package com.zhangyichuang.medicine.admin.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 订单取消请求参数
 *
 * @author Chuang
 */
@Data
@Schema(description = "订单取消请求参数")
public class OrderCancelRequest {

    @Schema(description = "订单ID", example = "1")
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @Schema(description = "取消原因", example = "用户不想要了")
    private String cancelReason;
}

