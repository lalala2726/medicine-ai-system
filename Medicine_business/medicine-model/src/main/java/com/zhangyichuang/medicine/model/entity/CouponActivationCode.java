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
 * 优惠券激活码实体。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("coupon_activation_code")
public class CouponActivationCode implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 激活码ID。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 激活码批次ID。
     */
    private Long batchId;

    /**
     * 激活码哈希值。
     */
    private String codeHash;

    /**
     * 激活码明文。
     */
    private String plainCode;

    /**
     * 激活码状态。
     */
    private String status;

    /**
     * 成功使用次数。
     */
    private Integer successUseCount;

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
