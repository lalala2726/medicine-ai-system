package com.zhangyichuang.medicine.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 客户端智能体订单卡摘要 DTO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "客户端智能体订单卡摘要")
public class ClientAgentOrderCardSummaryDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 订单编号。
     */
    @Schema(description = "订单编号")
    private String orderNo;

    /**
     * 订单状态编码。
     */
    @Schema(description = "订单状态编码")
    private String orderStatus;

    /**
     * 订单状态中文文案。
     */
    @Schema(description = "订单状态中文文案")
    private String orderStatusText;

    /**
     * 首个商品预览信息。
     */
    @Schema(description = "首个商品预览信息")
    private PreviewProduct previewProduct;

    /**
     * 订单商品总件数。
     */
    @Schema(description = "订单商品总件数")
    private Integer productCount;

    /**
     * 实付金额。
     */
    @Schema(description = "实付金额")
    private BigDecimal payAmount;

    /**
     * 订单总金额。
     */
    @Schema(description = "订单总金额")
    private BigDecimal totalAmount;

    /**
     * 下单时间。
     */
    @Schema(description = "下单时间")
    private Date createTime;

    /**
     * 订单卡首个商品预览。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "订单卡首个商品预览")
    public static class PreviewProduct implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 商品 ID。
         */
        @Schema(description = "商品ID")
        private Long productId;

        /**
         * 商品名称。
         */
        @Schema(description = "商品名称")
        private String productName;

        /**
         * 商品首图 URL。
         */
        @Schema(description = "商品首图URL")
        private String imageUrl;
    }
}
