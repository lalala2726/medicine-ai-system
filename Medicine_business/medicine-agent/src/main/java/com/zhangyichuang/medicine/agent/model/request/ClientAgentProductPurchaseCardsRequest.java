package com.zhangyichuang.medicine.agent.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 客户端智能体商品购买卡片请求。
 */
@Data
@Schema(description = "客户端智能体商品购买卡片请求")
public class ClientAgentProductPurchaseCardsRequest {

    @Valid
    @NotEmpty(message = "商品项不能为空")
    @Schema(description = "商品项列表")
    private List<PurchaseItem> items;

    @Data
    @Schema(description = "商品购买项")
    public static class PurchaseItem {

        @NotNull(message = "商品ID不能为空")
        @Min(value = 1, message = "商品ID必须大于0")
        @Schema(description = "商品ID", example = "101")
        private Long productId;

        @NotNull(message = "购买数量不能为空")
        @Min(value = 1, message = "购买数量必须大于0")
        @Schema(description = "购买数量", example = "2")
        private Integer quantity;
    }
}
