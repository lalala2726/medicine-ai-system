package com.zhangyichuang.medicine.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 订单配送物流表
 *
 * @author Chuang
 * created 2025/11/08
 */
@TableName(value = "mall_order_shipping")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MallOrderShipping {

    /**
     * 物流记录ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联订单ID
     */
    private Long orderId;

    /**
     * 物流单号
     */
    private String shippingNo;

    /**
     * 物流公司
     */
    private String shippingCompany;

    /**
     * 配送方式（同 mall_order.delivery_type）
     */
    private String deliveryType;

    /**
     * 发货人姓名
     */
    private String senderName;

    /**
     * 发货人电话
     */
    private String senderPhone;

    /**
     * 配送状态（0未发货,1运输中,2已签收,3异常）
     */
    private String status;

    /**
     * 物流信息详情(JSON结构)
     */
    private String shippingInfo;

    /**
     * 发货时间
     */
    private Date deliverTime;

    /**
     * 签收时间
     */
    private Date receiveTime;

    /**
     * 发货备注
     */
    private String shipmentNote;

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

