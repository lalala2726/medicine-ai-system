package com.zhangyichuang.medicine.admin.model.request;

import com.zhangyichuang.medicine.common.core.base.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 管理端用户优惠券列表查询请求。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "管理端用户优惠券列表查询请求")
public class AdminUserCouponListRequest extends PageRequest {

    /**
     * 用户ID。
     */
    @Schema(description = "用户ID", example = "1001")
    private Long userId;

    /**
     * 优惠券状态。
     */
    @Schema(description = "优惠券状态", example = "AVAILABLE")
    private String couponStatus;

    /**
     * 优惠券名称。
     */
    @Schema(description = "优惠券名称", example = "新人100元券")
    private String couponName;

    /**
     * 锁定订单号。
     */
    @Schema(description = "锁定订单号", example = "O202604061230000001")
    private String lockOrderNo;
}
