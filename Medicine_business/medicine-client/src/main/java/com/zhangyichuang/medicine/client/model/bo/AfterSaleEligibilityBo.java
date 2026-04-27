package com.zhangyichuang.medicine.client.model.bo;

import com.zhangyichuang.medicine.model.entity.MallOrder;
import com.zhangyichuang.medicine.model.entity.MallOrderItem;
import com.zhangyichuang.medicine.model.enums.AfterSaleScopeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 售后资格计算业务对象集合。
 *
 * <p>
 * 功能描述：统一承载售后资格校验过程中使用的命令对象、失败对象、明细结果对象与快照对象，
 * 仅用于服务内部业务计算，不直接返回前端。
 * </p>
 */
public final class AfterSaleEligibilityBo {

    /**
     * 工具类不允许实例化。
     */
    private AfterSaleEligibilityBo() {
    }

    /**
     * 售后资格计算命令 BO。
     */
    @Data
    @Builder
    public static class CommandBo {

        /**
         * 当前待校验的订单编号。
         */
        private String orderNo;

        /**
         * 前端请求的售后范围。
         */
        private AfterSaleScopeEnum requestedScope;

        /**
         * 前端指定的订单项 ID。
         */
        private Long requestedOrderItemId;

        /**
         * 当前登录用户 ID。
         */
        private Long userId;
    }

    /**
     * 售后资格失败结果 BO。
     */
    @Data
    @Builder
    public static class FailureBo {

        /**
         * 失败原因编码。
         */
        private String reasonCode;

        /**
         * 失败原因说明。
         */
        private String reasonMessage;
    }

    /**
     * 单个订单项的售后资格结果 BO。
     */
    @Data
    @Builder
    public static class ItemResultBo {

        /**
         * 当前参与校验的订单项实体。
         */
        private MallOrderItem orderItem;

        /**
         * 当前订单项是否满足售后条件。
         */
        private Boolean eligible;

        /**
         * 当前订单项的结果编码。
         */
        private String reasonCode;

        /**
         * 当前订单项的结果说明。
         */
        private String reasonMessage;

        /**
         * 当前订单项可退款金额。
         */
        private BigDecimal refundableAmount;
    }

    /**
     * 售后资格快照 BO。
     */
    @Data
    @Builder
    @AllArgsConstructor
    public static class SnapshotBo {

        /**
         * 当前快照对应的订单编号。
         */
        private String orderNo;

        /**
         * 前端请求的售后范围。
         */
        private AfterSaleScopeEnum requestedScope;

        /**
         * 后端最终解析后的售后范围。
         */
        private AfterSaleScopeEnum resolvedScope;

        /**
         * 前端传入的订单项 ID。
         */
        private Long requestedOrderItemId;

        /**
         * 当前快照最终选中的订单项 ID。
         */
        private Long selectedOrderItemId;

        /**
         * 订单状态编码。
         */
        private String orderStatus;

        /**
         * 订单状态名称。
         */
        private String orderStatusName;

        /**
         * 当前请求是否满足售后资格。
         */
        private Boolean eligible;

        /**
         * 当前快照的结果编码。
         */
        private String reasonCode;

        /**
         * 当前快照的结果说明。
         */
        private String reasonMessage;

        /**
         * 当前选中范围可退款金额。
         */
        private BigDecimal selectedRefundableAmount;

        /**
         * 当前整单可退款金额。
         */
        private BigDecimal totalRefundableAmount;

        /**
         * 当前订单的售后截止时间。
         */
        private Date afterSaleDeadlineTime;

        /**
         * 当前快照关联的订单实体。
         */
        private MallOrder order;

        /**
         * 当前订单下的全部订单项列表。
         */
        private List<MallOrderItem> orderItems;

        /**
         * 当前订单项维度的资格结果列表。
         */
        private List<ItemResultBo> itemResults;
    }
}
