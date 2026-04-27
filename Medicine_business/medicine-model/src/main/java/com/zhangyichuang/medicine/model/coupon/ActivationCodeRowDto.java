package com.zhangyichuang.medicine.model.coupon;

import lombok.Data;

import java.util.Date;

/**
 * 激活码明细查询结果。
 */
@Data
public class ActivationCodeRowDto {

    /**
     * 激活码ID。
     */
    private Long id;

    /**
     * 批次ID。
     */
    private Long batchId;

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
     * 创建时间。
     */
    private Date createTime;

    /**
     * 最近一次成功激活时间。
     */
    private Date lastSuccessTime;

    /**
     * 最近一次成功激活客户端IP。
     */
    private String lastSuccessClientIp;

    /**
     * 最近一次成功激活用户ID。
     */
    private Long lastSuccessUserId;
}
