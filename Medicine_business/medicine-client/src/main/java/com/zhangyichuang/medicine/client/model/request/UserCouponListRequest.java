package com.zhangyichuang.medicine.client.model.request;

import com.zhangyichuang.medicine.common.core.base.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户优惠券列表查询请求。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户优惠券列表查询请求")
public class UserCouponListRequest extends PageRequest {

    /**
     * 优惠券状态。
     */
    @Schema(description = "优惠券状态，仅支持 AVAILABLE、USED、EXPIRED", example = "AVAILABLE")
    private String couponStatus;
}
