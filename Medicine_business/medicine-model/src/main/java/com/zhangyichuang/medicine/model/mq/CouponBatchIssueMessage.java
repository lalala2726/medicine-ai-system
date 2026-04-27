package com.zhangyichuang.medicine.model.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 优惠券批量发券消息体。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponBatchIssueMessage implements Serializable {

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
     * 发券目标类型。
     */
    private String issueTargetType;

    /**
     * 指定用户ID列表。
     */
    private List<Long> userIds;

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
