package com.zhangyichuang.medicine.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 优惠券激活码批次实体。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("coupon_activation_batch")
public class CouponActivationBatch implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 激活码批次ID。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 批次号。
     */
    private String batchNo;

    /**
     * 优惠券模板ID。
     */
    private Long templateId;

    /**
     * 兑换规则类型。
     */
    private String redeemRuleType;

    /**
     * 有效期类型。
     */
    private String validityType;

    /**
     * 固定生效时间。
     */
    private Date fixedEffectiveTime;

    /**
     * 固定失效时间。
     */
    private Date fixedExpireTime;

    /**
     * 激活后有效天数。
     */
    private Integer relativeValidDays;

    /**
     * 批次状态。
     */
    private String status;

    /**
     * 生成数量。
     */
    private Integer generateCount;

    /**
     * 成功使用次数。
     */
    private Integer successUseCount;

    /**
     * 备注。
     */
    private String remark;

    /**
     * 乐观锁版本号。
     */
    @Version
    private Integer version;

    /**
     * 创建时间。
     */
    private Date createTime;

    /**
     * 更新时间。
     */
    private Date updateTime;

    /**
     * 创建人。
     */
    private String createBy;

    /**
     * 更新人。
     */
    private String updateBy;

    /**
     * 逻辑删除标记。
     */
    @TableLogic
    private Integer isDeleted;
}
