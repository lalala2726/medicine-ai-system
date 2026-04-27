package com.zhangyichuang.medicine.admin.model.request;

import com.zhangyichuang.medicine.common.core.base.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 优惠券模板列表查询请求。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "优惠券模板列表查询请求")
public class CouponTemplateListRequest extends PageRequest {

    /**
     * 优惠券名称。
     */
    @Schema(description = "优惠券名称", example = "新人100元券")
    private String name;

    /**
     * 模板状态。
     */
    @Schema(description = "模板状态", example = "ACTIVE")
    private String status;
}
