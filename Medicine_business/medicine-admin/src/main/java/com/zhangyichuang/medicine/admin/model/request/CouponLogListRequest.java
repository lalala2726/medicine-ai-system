package com.zhangyichuang.medicine.admin.model.request;

import com.zhangyichuang.medicine.common.core.base.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 优惠券日志列表查询请求。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "优惠券日志列表查询请求")
public class CouponLogListRequest extends PageRequest {

    /**
     * 用户优惠券ID。
     */
    @Schema(description = "用户优惠券ID", example = "10001")
    private Long couponId;

    /**
     * 用户ID。
     */
    @Schema(description = "用户ID", example = "1001")
    private Long userId;

    /**
     * 订单号。
     */
    @Schema(description = "订单号", example = "O202604061230000001")
    private String orderNo;

    /**
     * 变更类型。
     */
    @Schema(description = "变更类型", example = "LOCK")
    private String changeType;
}
