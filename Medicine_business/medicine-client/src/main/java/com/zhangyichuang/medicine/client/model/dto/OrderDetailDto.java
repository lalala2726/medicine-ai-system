package com.zhangyichuang.medicine.client.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 订单详情数据库查询DTO
 * 用于MyBatis映射查询结果，包含数据库中的原始字段
 *
 * @author Chuang
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "订单详情查询DTO")
public class OrderDetailDto {

    @Schema(description = "订单ID")
    private Long id;

    @Schema(description = "订单编号")
    private String orderNo;

    @Schema(description = "订单状态")
    private String orderStatus;

    @Schema(description = "订单总金额")
    private BigDecimal totalAmount;

    @Schema(description = "实际支付金额")
    private BigDecimal payAmount;

    @Schema(description = "商品原始总金额")
    private BigDecimal itemsAmount;

    @Schema(description = "运费金额")
    private BigDecimal freightAmount;

    @Schema(description = "使用的优惠券ID")
    private Long couponId;

    @Schema(description = "优惠券名称")
    private String couponName;

    @Schema(description = "订单抵扣金额")
    private BigDecimal couponDeductAmount;

    @Schema(description = "优惠券消耗金额")
    private BigDecimal couponConsumeAmount;

    @Schema(description = "支付方式")
    private String payType;

    @Schema(description = "配送方式")
    private String deliveryType;

    @Schema(description = "是否已支付(0-否, 1-是)")
    private Integer paid;

    @Schema(description = "支付过期时间")
    private Date payExpireTime;

    @Schema(description = "支付时间")
    private Date payTime;

    @Schema(description = "发货时间")
    private Date deliverTime;

    @Schema(description = "确认收货时间")
    private Date receiveTime;

    @Schema(description = "完成时间")
    private Date finishTime;

    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "用户留言")
    private String note;

    @Schema(description = "是否存在售后")
    private String afterSaleFlag;

    @Schema(description = "退款状态")
    private String refundStatus;

    @Schema(description = "退款金额")
    private BigDecimal refundPrice;

    @Schema(description = "退款时间")
    private Date refundTime;

    @Schema(description = "收货人姓名")
    private String receiverName;

    @Schema(description = "收货人电话")
    private String receiverPhone;

    @Schema(description = "收货详细地址")
    private String receiverDetail;
}
