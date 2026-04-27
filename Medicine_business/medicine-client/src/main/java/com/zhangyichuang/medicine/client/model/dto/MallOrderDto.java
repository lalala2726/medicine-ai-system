package com.zhangyichuang.medicine.client.model.dto;

import com.zhangyichuang.medicine.model.enums.OrderItemAfterSaleStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 商城订单DTO（用于数据库查询结果映射）
 *
 * @author Chuang
 * created 2025/11/19
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MallOrderDto {

    /**
     * 订单ID
     */
    private Long id;

    /**
     * 订单编号
     */
    private String orderNo;

    /**
     * 下单用户ID
     */
    private Long userId;

    /**
     * 商品原始总金额
     */
    private BigDecimal itemsAmount;

    /**
     * 订单总金额
     */
    private BigDecimal totalAmount;

    /**
     * 实际支付金额
     */
    private BigDecimal payAmount;

    /**
     * 运费金额
     */
    private BigDecimal freightAmount;

    /**
     * 使用的优惠券ID
     */
    private Long couponId;

    /**
     * 优惠券名称
     */
    private String couponName;

    /**
     * 订单抵扣金额
     */
    private BigDecimal couponDeductAmount;

    /**
     * 优惠券消耗金额
     */
    private BigDecimal couponConsumeAmount;

    /**
     * 优惠券浪费金额
     */
    private BigDecimal couponWasteAmount;

    /**
     * 支付方式
     */
    private String payType;

    /**
     * 订单状态
     */
    private String orderStatus;

    /**
     * 配送方式
     */
    private String deliveryType;

    /**
     * 用户收货地址ID
     */
    private Long addressId;

    /**
     * 收货人姓名
     */
    private String receiverName;

    /**
     * 收货人电话
     */
    private String receiverPhone;

    /**
     * 收货详细地址
     */
    private String receiverDetail;

    /**
     * 用户留言
     */
    private String note;

    /**
     * 支付过期时间
     */
    private Date payExpireTime;

    /**
     * 退款状态
     */
    private String refundStatus;

    /**
     * 退款时间
     */
    private Date refundTime;

    /**
     * 退款金额
     */
    private BigDecimal refundPrice;

    /**
     * 是否存在售后
     */
    private OrderItemAfterSaleStatusEnum afterSaleFlag;

    /**
     * 支付时间
     */
    private Date payTime;

    /**
     * 是否支付
     */
    private Integer paid;

    /**
     * 发货时间
     */
    private Date deliverTime;

    /**
     * 确认收货时间
     */
    private Date receiveTime;

    /**
     * 完成时间
     */
    private Date finishTime;

    /**
     * 订单关闭原因
     */
    private String closeReason;

    /**
     * 订单关闭时间
     */
    private Date closeTime;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 乐观锁版本号
     */
    private Integer version;

    /**
     * 修改时间
     */
    private Date updateTime;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 修改人
     */
    private String updateBy;

    /**
     * 是否删除
     */
    private Integer isDeleted;

    /**
     * 备注
     */
    private String remark;
}
