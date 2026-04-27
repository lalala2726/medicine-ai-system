package com.zhangyichuang.medicine.agent.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 客户端智能体批量药品详情请求。
 */
@Data
@Schema(description = "客户端智能体批量药品详情请求")
public class ClientAgentProductDetailsRequest {

    /**
     * 商品ID列表。
     */
    @NotEmpty(message = "商品ID列表不能为空")
    @Schema(description = "商品ID列表", example = "[1,2,3]")
    private List<@NotNull(message = "商品ID不能为空") @Min(value = 1, message = "商品ID必须大于0") Long> productIds;
}
