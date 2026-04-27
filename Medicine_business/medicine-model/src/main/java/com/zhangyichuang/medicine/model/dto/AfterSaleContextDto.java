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
import java.util.List;

/**
 * 管理端智能体售后聚合上下文 DTO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "售后聚合上下文")
public class AfterSaleContextDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 售后单号。
     */
    @Schema(description = "售后单号")
    private String afterSaleNo;

    /**
     * 关联订单编号。
     */
    @Schema(description = "关联订单编号")
    private String orderNo;

    /**
     * 售后状态编码。
     */
    @Schema(description = "售后状态编码")
    private String statusCode;

    /**
     * 售后状态中文名称。
     */
    @Schema(description = "售后状态中文名称")
    private String statusText;

    /**
     * 售后类型编码。
     */
    @Schema(description = "售后类型编码")
    private String typeCode;

    /**
     * 售后类型中文名称。
     */
    @Schema(description = "售后类型中文名称")
    private String typeText;

    /**
     * 退款金额。
     */
    @Schema(description = "退款金额")
    private BigDecimal refundAmount;

    /**
     * 申请原因中文文案。
     */
    @Schema(description = "申请原因中文文案")
    private String reasonText;

    /**
     * 售后商品摘要。
     */
    @Schema(description = "售后商品摘要")
    private ProductSummary productSummary;

    /**
     * 凭证摘要。
     */
    @Schema(description = "凭证摘要")
    private EvidenceSummary evidenceSummary;

    /**
     * 最近处理时间线摘要。
     */
    @Schema(description = "最近处理时间线摘要")
    private List<TimelineItem> timelineSummary;

    /**
     * 智能体决策提示。
     */
    @Schema(description = "智能体决策提示")
    private AiHints aiHints;

    /**
     * 售后商品摘要。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "售后商品摘要")
    public static class ProductSummary implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 商品 ID。
         */
        @Schema(description = "商品 ID")
        private Long productId;

        /**
         * 商品名称。
         */
        @Schema(description = "商品名称")
        private String productName;

        /**
         * 商品图片。
         */
        @Schema(description = "商品图片")
        private String productImage;

        /**
         * 售后商品数量。
         */
        @Schema(description = "售后商品数量")
        private Integer quantity;

        /**
         * 商品总价。
         */
        @Schema(description = "商品总价")
        private BigDecimal totalPrice;
    }

    /**
     * 售后凭证摘要。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "售后凭证摘要")
    public static class EvidenceSummary implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 凭证图片数量。
         */
        @Schema(description = "凭证图片数量")
        private Integer evidenceCount;

        /**
         * 首张凭证图片。
         */
        @Schema(description = "首张凭证图片")
        private String firstEvidenceImage;
    }

    /**
     * 售后时间线条目摘要。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "售后时间线条目摘要")
    public static class TimelineItem implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 事件类型编码。
         */
        @Schema(description = "事件类型编码")
        private String eventType;

        /**
         * 事件类型中文名称。
         */
        @Schema(description = "事件类型中文名称")
        private String eventTypeName;

        /**
         * 事件状态编码。
         */
        @Schema(description = "事件状态编码")
        private String eventStatus;

        /**
         * 操作方类型编码。
         */
        @Schema(description = "操作方类型编码")
        private String operatorType;

        /**
         * 操作方类型中文名称。
         */
        @Schema(description = "操作方类型中文名称")
        private String operatorTypeName;

        /**
         * 事件描述。
         */
        @Schema(description = "事件描述")
        private String description;

        /**
         * 事件发生时间。
         */
        @Schema(description = "事件发生时间")
        private Date eventTime;
    }

    /**
     * 售后智能体决策提示。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "售后智能体决策提示")
    public static class AiHints implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 是否等待审核。
         */
        @Schema(description = "是否等待审核")
        private Boolean waitingAudit;

        /**
         * 是否处理中。
         */
        @Schema(description = "是否处理中")
        private Boolean processing;

        /**
         * 是否已完成。
         */
        @Schema(description = "是否已完成")
        private Boolean completed;

        /**
         * 是否已拒绝。
         */
        @Schema(description = "是否已拒绝")
        private Boolean rejected;

        /**
         * 是否可取消。
         */
        @Schema(description = "是否可取消")
        private Boolean canCancel;

        /**
         * 是否可重新申请。
         */
        @Schema(description = "是否可重新申请")
        private Boolean canReapply;
    }
}
