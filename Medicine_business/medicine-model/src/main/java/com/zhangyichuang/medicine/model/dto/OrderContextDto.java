package com.zhangyichuang.medicine.model.dto;

import com.zhangyichuang.medicine.common.core.annotation.DataMasking;
import com.zhangyichuang.medicine.common.core.enums.MaskingType;
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
 * 管理端智能体订单聚合上下文 DTO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "订单聚合上下文")
public class OrderContextDto implements Serializable {

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
    private String statusCode;

    /**
     * 订单状态中文名称。
     */
    @Schema(description = "订单状态中文名称")
    private String statusText;

    /**
     * 支付方式编码。
     */
    @Schema(description = "支付方式编码")
    private String payType;

    /**
     * 金额摘要。
     */
    @Schema(description = "金额摘要")
    private AmountSummary amountSummary;

    /**
     * 收货摘要。
     */
    @Schema(description = "收货摘要")
    private ReceiverSummary receiverSummary;

    /**
     * 商品摘要。
     */
    @Schema(description = "商品摘要")
    private ProductSummary productSummary;

    /**
     * 发货与物流完整信息。
     */
    @Schema(description = "发货与物流完整信息")
    private ShippingSummary shippingSummary;

    /**
     * 完整订单时间线。
     */
    @Schema(description = "完整订单时间线")
    private List<TimelineItem> timeline;

    /**
     * 智能体决策提示。
     */
    @Schema(description = "智能体决策提示")
    private AiHints aiHints;

    /**
     * 订单金额摘要。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "订单金额摘要")
    public static class AmountSummary implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 订单总金额。
         */
        @Schema(description = "订单总金额")
        private BigDecimal totalAmount;

        /**
         * 实际支付金额。
         */
        @Schema(description = "实际支付金额")
        private BigDecimal payAmount;

        /**
         * 运费金额。
         */
        @Schema(description = "运费金额")
        private BigDecimal freightAmount;
    }

    /**
     * 订单收货摘要。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "订单收货摘要")
    public static class ReceiverSummary implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 收货人姓名。
         */
        @Schema(description = "收货人姓名")
        private String receiverName;

        /**
         * 脱敏后的收货手机号。
         */
        @DataMasking(type = MaskingType.MOBILE_PHONE)
        @Schema(description = "脱敏后的收货手机号")
        private String receiverPhone;

        /**
         * 配送方式。
         */
        @Schema(description = "配送方式")
        private String deliveryMethod;

        /**
         * 完整收货地址。
         */
        @Schema(description = "完整收货地址")
        private String receiverAddress;
    }

    /**
     * 订单商品摘要。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "订单商品摘要")
    public static class ProductSummary implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 商品总数量。
         */
        @Schema(description = "商品总数量")
        private Integer productCount;

        /**
         * 商品明细行数。
         */
        @Schema(description = "商品明细行数")
        private Integer productLineCount;

        /**
         * 前三个商品名称。
         */
        @Schema(description = "前三个商品名称")
        private List<String> topProductNames;

        /**
         * 是否存在售后。
         */
        @Schema(description = "是否存在售后")
        private Boolean hasAfterSale;
    }

    /**
     * 订单发货与物流完整信息。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "订单发货与物流完整信息")
    public static class ShippingSummary implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 是否已发货。
         */
        @Schema(description = "是否已发货")
        private Boolean shipped;

        /**
         * 物流公司。
         */
        @Schema(description = "物流公司")
        private String logisticsCompany;

        /**
         * 物流单号。
         */
        @Schema(description = "物流单号")
        private String trackingNumber;

        /**
         * 发货备注。
         */
        @Schema(description = "发货备注")
        private String shipmentNote;

        /**
         * 物流状态编码。
         */
        @Schema(description = "物流状态编码")
        private String statusCode;

        /**
         * 物流状态中文名称。
         */
        @Schema(description = "物流状态中文名称")
        private String statusText;

        /**
         * 发货时间。
         */
        @Schema(description = "发货时间")
        private Date deliverTime;

        /**
         * 收货时间。
         */
        @Schema(description = "收货时间")
        private Date receiveTime;

        /**
         * 完整物流轨迹节点。
         */
        @Schema(description = "完整物流轨迹节点")
        private List<ShippingNodeSummary> nodes;
    }

    /**
     * 物流节点摘要。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "物流节点摘要")
    public static class ShippingNodeSummary implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 节点时间。
         */
        @Schema(description = "节点时间")
        private String time;

        /**
         * 节点内容。
         */
        @Schema(description = "节点内容")
        private String content;

        /**
         * 节点位置。
         */
        @Schema(description = "节点位置")
        private String location;
    }

    /**
     * 订单时间线条目摘要。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "订单时间线条目摘要")
    public static class TimelineItem implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 时间线记录 ID。
         */
        @Schema(description = "时间线记录 ID")
        private Long id;

        /**
         * 事件类型。
         */
        @Schema(description = "事件类型")
        private String eventType;

        /**
         * 事件状态。
         */
        @Schema(description = "事件状态")
        private String eventStatus;

        /**
         * 操作方类型。
         */
        @Schema(description = "操作方类型")
        private String operatorType;

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
     * 订单智能体决策提示。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "订单智能体决策提示")
    public static class AiHints implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 是否可取消订单。
         */
        @Schema(description = "是否可取消订单")
        private Boolean canCancel;

        /**
         * 是否可申请售后。
         */
        @Schema(description = "是否可申请售后")
        private Boolean canApplyAfterSale;

        /**
         * 是否需要用户支付。
         */
        @Schema(description = "是否需要用户支付")
        private Boolean needsPayment;

        /**
         * 是否需要商家发货。
         */
        @Schema(description = "是否需要商家发货")
        private Boolean needsShipment;

        /**
         * 是否需要用户收货。
         */
        @Schema(description = "是否需要用户收货")
        private Boolean needsReceipt;

        /**
         * 是否存在售后。
         */
        @Schema(description = "是否存在售后")
        private Boolean hasAfterSale;
    }
}
