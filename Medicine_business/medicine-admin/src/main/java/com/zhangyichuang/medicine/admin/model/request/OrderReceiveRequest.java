package com.zhangyichuang.medicine.admin.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


/**
 * 管理员确认收货请求
 *
 * @author Chuang
 * created 2025/11/08
 */
@Data
@Schema(description = "管理员确认收货请求")
public class OrderReceiveRequest {

    @NotNull(message = "订单ID不能为空")
    @Schema(description = "订单ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long orderId;

    @Schema(description = "确认备注", example = "客户已电话确认收货")
    private String remark;
}

