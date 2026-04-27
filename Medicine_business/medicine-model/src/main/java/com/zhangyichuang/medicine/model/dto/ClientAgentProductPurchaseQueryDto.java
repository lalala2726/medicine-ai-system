package com.zhangyichuang.medicine.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 客户端智能体商品购买项 DTO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "客户端智能体商品购买项")
public class ClientAgentProductPurchaseQueryDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 商品ID。
     */
    @Schema(description = "商品ID", example = "101")
    private Long productId;

    /**
     * 购买数量。
     */
    @Schema(description = "购买数量", example = "2")
    private Integer quantity;
}
