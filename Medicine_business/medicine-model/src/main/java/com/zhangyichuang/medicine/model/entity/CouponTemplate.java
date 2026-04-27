package com.zhangyichuang.medicine.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 优惠券模板实体。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("coupon_template")
public class CouponTemplate implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 优惠券模板ID。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 优惠券类型。
     */
    private String couponType;

    /**
     * 优惠券名称。
     */
    private String name;

    /**
     * 使用门槛金额。
     */
    private BigDecimal thresholdAmount;

    /**
     * 优惠券面额。
     */
    private BigDecimal faceAmount;

    /**
     * 是否允许继续使用：1-允许，0-不允许。
     */
    private Integer continueUseEnabled;

    /**
     * 是否允许叠加：1-允许，0-不允许。
     */
    private Integer stackableEnabled;

    /**
     * 模板状态。
     */
    private String status;

    /**
     * 模板备注。
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
