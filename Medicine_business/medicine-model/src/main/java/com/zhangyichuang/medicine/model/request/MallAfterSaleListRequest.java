package com.zhangyichuang.medicine.model.request;

import com.zhangyichuang.medicine.common.core.base.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 售后列表查询参数。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "售后列表查询参数")
public class MallAfterSaleListRequest extends PageRequest {

    @Schema(description = "售后类型(REFUND_ONLY/RETURN_REFUND/EXCHANGE)", example = "REFUND_ONLY")
    private String afterSaleType;

    @Schema(description = "售后状态(PENDING/APPROVED/REJECTED/PROCESSING/COMPLETED/CANCELLED)", example = "PENDING")
    private String afterSaleStatus;

    @Schema(description = "订单编号", example = "O20251108123456789012")
    private String orderNo;

    @Schema(description = "用户ID", example = "1")
    private Long userId;

    @Schema(description = "申请原因", example = "DAMAGED")
    private String applyReason;
}
