package com.zhangyichuang.medicine.admin.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @author Chuang
 * <p>
 * created on 2025/11/1
 */
@Data
@Schema(description = "订单备注更新参数")
public class RemarkUpdateRequest {

    @Schema(description = "订单ID", example = "123456")
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @Schema(description = "订单备注", example = "客户要求尽快发货")
    private String remark;

}
