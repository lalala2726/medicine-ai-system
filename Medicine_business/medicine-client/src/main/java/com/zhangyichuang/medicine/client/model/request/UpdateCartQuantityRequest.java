package com.zhangyichuang.medicine.client.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

/**
 * @author Chuang
 * <p>
 * created on 2025/11/11 17:05
 */
@Data
@Schema(description = "更新购物车请求参数")
public class UpdateCartQuantityRequest {

    @NotNull(message = "购物车ID不能为空")
    @Schema(description = "购物车ID")
    private Long cartId;

    @NotNull(message = "数量不能为空")
    @PositiveOrZero(message = "数量不能小于0")
    @Schema(description = "数量")
    private Integer quantity;
}
