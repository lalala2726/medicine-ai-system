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
import java.util.Date;

/**
 * 激活码发券日志实体。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("coupon_activation_grant_log")
public class CouponActivationGrantLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 发券日志ID。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 兑换日志ID。
     */
    private Long redeemLogId;

    /**
     * 批次ID。
     */
    private Long batchId;

    /**
     * 激活码ID。
     */
    private Long activationCodeId;

    /**
     * 优惠券模板ID。
     */
    private Long templateId;

    /**
     * 用户ID。
     */
    private Long userId;

    /**
     * 用户优惠券ID。
     */
    private Long couponId;

    /**
     * 来源类型。
     */
    private String sourceType;

    /**
     * 来源业务号。
     */
    private String sourceBizNo;

    /**
     * 发券方式。
     */
    private String grantMode;

    /**
     * 发券状态。
     */
    private String grantStatus;

    /**
     * 发券错误编码。
     */
    private String grantErrorCode;

    /**
     * 发券错误信息。
     */
    private String grantErrorMessage;

    /**
     * 创建时间。
     */
    private Date createTime;

    /**
     * 逻辑删除标记。
     */
    @TableLogic
    private Integer isDeleted;
}
