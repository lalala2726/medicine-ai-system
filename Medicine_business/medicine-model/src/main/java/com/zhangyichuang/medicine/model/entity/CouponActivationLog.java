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
 * 优惠券激活码日志实体。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("coupon_activation_log")
public class CouponActivationLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 激活码日志ID。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 兑换请求ID。
     */
    private String requestId;

    /**
     * 批次ID。
     */
    private Long batchId;

    /**
     * 激活码ID。
     */
    private Long activationCodeId;

    /**
     * 激活码明文快照。
     */
    private String plainCodeSnapshot;

    /**
     * 兑换规则类型。
     */
    private String redeemRuleType;

    /**
     * 结果状态。
     */
    private String resultStatus;

    /**
     * 用户ID。
     */
    private Long userId;

    /**
     * 用户优惠券ID。
     */
    private Long couponId;

    /**
     * 失败编码。
     */
    private String failCode;

    /**
     * 失败信息。
     */
    private String failMessage;

    /**
     * 客户端IP。
     */
    private String clientIp;

    /**
     * 成功占位键。
     */
    private String successLockKey;

    /**
     * 发券方式。
     */
    private String grantMode;

    /**
     * 发券状态。
     */
    private String grantStatus;

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
