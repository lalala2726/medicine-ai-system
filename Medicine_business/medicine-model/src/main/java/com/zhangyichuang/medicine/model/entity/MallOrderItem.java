package com.zhangyichuang.medicine.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 商城订单明细表（商品项）
 */
@TableName(value = "mall_order_item")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MallOrderItem {

    /**
     * 订单项ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联 mall_order.id
     */
    private Long orderId;

    /**
     * 商城商品ID（mall_product）
     */
    private Long productId;

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 购买数量
     */
    private Integer quantity;

    /**
     * 单价
     */
    private BigDecimal price;

    /**
     * 小计金额
     */
    private BigDecimal totalPrice;

    /**
     * 分摊优惠金额。
     */
    private BigDecimal couponDeductAmount;

    /**
     * 分摊后应付金额。
     */
    private BigDecimal payableAmount;

    /**
     * 商品图片
     */
    private String imageUrl;

    /**
     * 售后状态(NONE-无售后, IN_PROGRESS-售后中, COMPLETED-售后完成)
     */
    private String afterSaleStatus;

    /**
     * 已退款金额
     */
    private BigDecimal refundedAmount;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 逻辑删除(0否,1是)
     */
    @TableLogic
    private Integer isDeleted;
}
