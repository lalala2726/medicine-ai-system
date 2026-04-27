package com.zhangyichuang.medicine.model.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 优惠券单用户发券消息体。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponIssueMessage implements Serializable {

    /**
     * 序列化版本号。
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 模板ID。
     */
    private Long templateId;

    /**
     * 用户ID。
     */
    private Long userId;

    /**
     * 生效时间。
     */
    private Date effectiveTime;

    /**
     * 失效时间。
     */
    private Date expireTime;

    /**
     * 来源业务号。
     */
    private String sourceBizNo;

    /**
     * 发券备注。
     */
    private String remark;

    /**
     * 操作人标识。
     */
    private String operatorId;
}
