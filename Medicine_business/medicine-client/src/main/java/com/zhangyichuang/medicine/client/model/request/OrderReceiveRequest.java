package com.zhangyichuang.medicine.client.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 确认收货请求参数
 *
 * @author Chuang
 * created 2025/11/08
 */
@Data
@Schema(description = "确认收货请求参数")
public class OrderReceiveRequest {

    @Schema(description = "订单编号", example = "O20251110123456789012")
    @NotBlank(message = "订单编号不能为空")
    private String orderNo;
}

