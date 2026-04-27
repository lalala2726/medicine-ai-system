package com.zhangyichuang.medicine.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 售后申请表
 *
 * @author Chuang
 * created 2025/11/08
 */
@TableName(value = "mall_after_sale")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MallAfterSale implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 售后申请ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 售后单号(业务唯一标识)
     */
    private String afterSaleNo;

    /**
     * 关联订单ID
     */
    private Long orderId;

    /**
     * 订单编号(冗余字段)
     */
    private String orderNo;

    /**
     * 关联订单项ID
     */
    private Long orderItemId;

    /**
     * 申请用户ID
     */
    private Long userId;

    /**
     * 售后类型(REFUND_ONLY-仅退款, RETURN_REFUND-退货退款, EXCHANGE-换货)
     */
    private String afterSaleType;

    /**
     * 售后状态(PENDING-待审核, APPROVED-已通过, REJECTED-已拒绝, PROCESSING-处理中, COMPLETED-已完成, CANCELLED-已取消)
     */
    private String afterSaleStatus;

    /**
     * 退款金额
     */
    private BigDecimal refundAmount;

    /**
     * 申请原因
     */
    private String applyReason;

    /**
     * 详细说明
     */
    private String applyDescription;

    /**
     * 凭证图片(JSON数组格式)
     */
    private String evidenceImages;

    /**
     * 收货状态(RECEIVED-已收到货, NOT_RECEIVED-未收到货)
     */
    private String receiveStatus;

    /**
     * 拒绝原因(审核拒绝时填写)
     */
    private String rejectReason;

    /**
     * 管理员备注
     */
    private String adminRemark;

    /**
     * 申请时间
     */
    private Date applyTime;

    /**
     * 审核时间
     */
    private Date auditTime;

    /**
     * 完成时间
     */
    private Date completeTime;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 更新人
     */
    private String updateBy;

    /**
     * 逻辑删除(0否,1是)
     */
    @TableLogic
    private Integer isDeleted;
}
