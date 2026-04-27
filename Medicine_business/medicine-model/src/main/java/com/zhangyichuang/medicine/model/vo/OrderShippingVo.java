package com.zhangyichuang.medicine.model.vo;

import com.zhangyichuang.medicine.common.core.annotation.DataMasking;
import com.zhangyichuang.medicine.common.core.enums.MaskingType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 订单物流信息VO
 *
 * @author Chuang
 * created 2025/11/08
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "订单物流信息VO")
public class OrderShippingVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "订单ID", example = "1")
    private Long orderId;

    @Schema(description = "订单号", example = "2025000000000001")
    private String orderNo;

    @Schema(description = "订单状态", example = "PENDING_RECEIPT")
    private String orderStatus;

    @Schema(description = "订单状态名称", example = "待收货")
    private String orderStatusName;

    @Schema(description = "物流公司", example = "顺丰速运")
    private String logisticsCompany;

    @Schema(description = "物流单号", example = "SF1234567890")
    private String trackingNumber;

    @Schema(description = "发货备注", example = "已发货，请注意查收")
    private String shipmentNote;

    @Schema(description = "发货时间", example = "2025-11-08 10:00:00")
    private Date deliverTime;

    @Schema(description = "签收时间", example = "2025-11-15 14:30:00")
    private Date receiveTime;

    @Schema(description = "物流状态", example = "IN_TRANSIT")
    private String status;

    @Schema(description = "物流状态名称", example = "运输中")
    private String statusName;

    @Schema(description = "收货人信息")
    private ReceiverInfo receiverInfo;

    @Schema(description = "物流轨迹节点")
    private List<ShippingNode> nodes;

    /**
     * 收货人信息内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "收货人信息")
    public static class ReceiverInfo implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "收货人姓名", example = "张三")
        private String receiverName;

        @Schema(description = "收货人电话", example = "13800138000")
        @DataMasking(type = MaskingType.MOBILE_PHONE)
        private String receiverPhone;

        @Schema(description = "收货详细地址", example = "广东省深圳市南山区科技园xxx号")
        private String receiverDetail;

        @Schema(description = "配送方式", example = "EXPRESS")
        private String deliveryType;

        @Schema(description = "配送方式名称", example = "快递配送")
        private String deliveryTypeName;
    }

    /**
     * 物流轨迹节点
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "物流轨迹节点")
    public static class ShippingNode implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "节点时间", example = "2025-11-08 10:00:00")
        private String time;

        @Schema(description = "节点内容", example = "快件已到达上海转运中心")
        private String content;

        @Schema(description = "节点位置", example = "上海")
        private String location;
    }
}
