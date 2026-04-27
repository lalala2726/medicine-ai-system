package com.zhangyichuang.medicine.agent.model.vo.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.zhangyichuang.medicine.agent.annotation.AgentCodeLabel;
import com.zhangyichuang.medicine.agent.annotation.FieldDescription;
import com.zhangyichuang.medicine.agent.mapping.AgentCodeLabelRegistry;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 客户端智能体售后详情。
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "客户端智能体售后详情")
@FieldDescription(description = "客户端智能体售后详情")
public class ClientAgentAfterSaleDetailVo {

    /**
     * 售后申请ID。
     */
    @Schema(description = "售后申请ID", example = "1")
    @FieldDescription(description = "售后申请ID")
    private Long id;

    /**
     * 售后单号。
     */
    @Schema(description = "售后单号", example = "AS20251108001")
    @FieldDescription(description = "售后单号")
    private String afterSaleNo;

    /**
     * 订单ID。
     */
    @Schema(description = "订单ID", example = "1001")
    @FieldDescription(description = "订单ID")
    private Long orderId;

    /**
     * 订单编号。
     */
    @Schema(description = "订单编号", example = "O20251108001")
    @FieldDescription(description = "订单编号")
    private String orderNo;

    /**
     * 订单项ID。
     */
    @Schema(description = "订单项ID", example = "2001")
    @FieldDescription(description = "订单项ID")
    private Long orderItemId;

    /**
     * 用户ID。
     */
    @Schema(description = "用户ID", example = "3001")
    @FieldDescription(description = "用户ID")
    private Long userId;

    /**
     * 用户昵称。
     */
    @Schema(description = "用户昵称", example = "张三")
    @FieldDescription(description = "用户昵称")
    private String userNickname;

    /**
     * 售后类型编码。
     */
    @Schema(description = "售后类型编码")
    @FieldDescription(description = "售后类型编码")
    @AgentCodeLabel(dictKey = AgentCodeLabelRegistry.AGENT_AFTER_SALE_TYPE)
    private String afterSaleType;

    /**
     * 售后类型名称。
     */
    @Schema(description = "售后类型名称", example = "仅退款")
    @FieldDescription(description = "售后类型名称")
    private String afterSaleTypeName;

    /**
     * 售后状态编码。
     */
    @Schema(description = "售后状态编码")
    @FieldDescription(description = "售后状态编码")
    @AgentCodeLabel(dictKey = AgentCodeLabelRegistry.AGENT_AFTER_SALE_STATUS)
    private String afterSaleStatus;

    /**
     * 售后状态名称。
     */
    @Schema(description = "售后状态名称", example = "待审核")
    @FieldDescription(description = "售后状态名称")
    private String afterSaleStatusName;

    /**
     * 退款金额。
     */
    @Schema(description = "退款金额", example = "99.99")
    @FieldDescription(description = "退款金额")
    private BigDecimal refundAmount;

    /**
     * 申请原因编码。
     */
    @Schema(description = "申请原因编码")
    @FieldDescription(description = "申请原因编码")
    @AgentCodeLabel(dictKey = AgentCodeLabelRegistry.AGENT_AFTER_SALE_REASON)
    private String applyReason;

    /**
     * 申请原因名称。
     */
    @Schema(description = "申请原因名称")
    @FieldDescription(description = "申请原因名称")
    private String applyReasonName;

    /**
     * 申请详细说明。
     */
    @Schema(description = "详细说明")
    @FieldDescription(description = "详细说明")
    private String applyDescription;

    /**
     * 凭证图片列表。
     */
    @Schema(description = "凭证图片列表")
    @FieldDescription(description = "凭证图片列表")
    private List<String> evidenceImages;

    /**
     * 收货状态编码。
     */
    @Schema(description = "收货状态编码")
    @FieldDescription(description = "收货状态编码")
    @AgentCodeLabel(dictKey = AgentCodeLabelRegistry.AGENT_AFTER_SALE_RECEIVE_STATUS)
    private String receiveStatus;

    /**
     * 收货状态名称。
     */
    @Schema(description = "收货状态名称")
    @FieldDescription(description = "收货状态名称")
    private String receiveStatusName;

    /**
     * 驳回原因。
     */
    @Schema(description = "拒绝原因")
    @FieldDescription(description = "拒绝原因")
    private String rejectReason;

    /**
     * 管理员备注。
     */
    @Schema(description = "管理员备注")
    @FieldDescription(description = "管理员备注")
    private String adminRemark;

    /**
     * 申请时间。
     */
    @Schema(description = "申请时间")
    @FieldDescription(description = "申请时间")
    private Date applyTime;

    /**
     * 审核时间。
     */
    @Schema(description = "审核时间")
    @FieldDescription(description = "审核时间")
    private Date auditTime;

    /**
     * 完成时间。
     */
    @Schema(description = "完成时间")
    @FieldDescription(description = "完成时间")
    private Date completeTime;

    /**
     * 商品信息。
     */
    @Schema(description = "商品信息")
    @FieldDescription(description = "商品信息")
    private ProductInfo productInfo;

    /**
     * 售后时间线列表。
     */
    @Schema(description = "时间线列表")
    @FieldDescription(description = "时间线列表")
    private List<TimelineItem> timeline;

    @Data
    @Schema(description = "商品信息")
    @FieldDescription(description = "商品信息")
    public static class ProductInfo {

        /**
         * 商品ID。
         */
        @Schema(description = "商品ID", example = "4001")
        @FieldDescription(description = "商品ID")
        private Long productId;

        /**
         * 商品名称。
         */
        @Schema(description = "商品名称", example = "医用口罩")
        @FieldDescription(description = "商品名称")
        private String productName;

        /**
         * 商品图片地址。
         */
        @Schema(description = "商品图片")
        @FieldDescription(description = "商品图片")
        private String productImage;

        /**
         * 商品单价。
         */
        @Schema(description = "商品单价")
        @FieldDescription(description = "商品单价")
        private BigDecimal productPrice;

        /**
         * 购买数量。
         */
        @Schema(description = "购买数量")
        @FieldDescription(description = "购买数量")
        private Integer quantity;

        /**
         * 小计金额。
         */
        @Schema(description = "小计金额")
        @FieldDescription(description = "小计金额")
        private BigDecimal totalPrice;
    }

    @Data
    @Schema(description = "售后时间线")
    @FieldDescription(description = "售后时间线")
    public static class TimelineItem {

        /**
         * 时间线ID。
         */
        @Schema(description = "时间线ID", example = "1")
        @FieldDescription(description = "时间线ID")
        private Long id;

        /**
         * 事件类型编码。
         */
        @Schema(description = "事件类型编码")
        @FieldDescription(description = "事件类型编码")
        @AgentCodeLabel(dictKey = AgentCodeLabelRegistry.AGENT_ORDER_EVENT_TYPE)
        private String eventType;

        /**
         * 事件类型名称。
         */
        @Schema(description = "事件类型名称")
        @FieldDescription(description = "事件类型名称")
        private String eventTypeName;

        /**
         * 事件状态。
         */
        @Schema(description = "事件状态")
        @FieldDescription(description = "事件状态")
        private String eventStatus;

        /**
         * 操作方编码。
         */
        @Schema(description = "操作方编码")
        @FieldDescription(description = "操作方编码")
        @AgentCodeLabel(dictKey = AgentCodeLabelRegistry.AGENT_OPERATOR_TYPE)
        private String operatorType;

        /**
         * 操作方名称。
         */
        @Schema(description = "操作方名称")
        @FieldDescription(description = "操作方名称")
        private String operatorTypeName;

        /**
         * 事件描述。
         */
        @Schema(description = "事件描述")
        @FieldDescription(description = "事件描述")
        private String description;

        /**
         * 创建时间。
         */
        @Schema(description = "创建时间")
        @FieldDescription(description = "创建时间")
        private Date createTime;
    }
}
