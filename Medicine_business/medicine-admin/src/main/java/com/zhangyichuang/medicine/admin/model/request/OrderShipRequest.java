package com.zhangyichuang.medicine.admin.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 订单发货请求参数
 *
 * @author Chuang
 * created 2025/11/08
 */
@Data
@Schema(description = "订单发货请求参数")
public class OrderShipRequest {
    @Schema(description = "订单ID", example = "1")
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @Schema(description = "物流公司", example = "顺丰速运")
    @NotBlank(message = "物流公司不能为空")
    private String logisticsCompany;

    @Schema(description = "物流单号", example = "SF1234567890")
    @NotBlank(message = "物流单号不能为空")
    private String trackingNumber;

    @Schema(description = "发货备注", example = "已发货，请注意查收")
    private String shipmentNote;
}

