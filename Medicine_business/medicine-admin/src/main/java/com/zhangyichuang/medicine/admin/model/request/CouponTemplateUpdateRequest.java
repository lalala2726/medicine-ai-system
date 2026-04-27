package com.zhangyichuang.medicine.admin.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 优惠券模板修改请求。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "优惠券模板修改请求")
public class CouponTemplateUpdateRequest extends CouponTemplateAddRequest {

    /**
     * 模板ID。
     */
    @NotNull(message = "优惠券ID不能为空")
    @Schema(description = "模板ID", example = "1")
    private Long id;
}
