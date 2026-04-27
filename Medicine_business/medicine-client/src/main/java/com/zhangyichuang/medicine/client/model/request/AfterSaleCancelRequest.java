package com.zhangyichuang.medicine.client.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * 用户取消售后请求
 *
 * @author Chuang
 * created 2025/11/08
 */
@Data
@Schema(description = "取消售后请求")
public class AfterSaleCancelRequest {

    @NotNull(message = "售后申请ID不能为空")
    @Positive(message = "售后申请ID必须为正数")
    @Schema(description = "售后申请ID", example = "1")
    private Long afterSaleId;

    @Schema(description = "取消原因", example = "不需要售后了")
    private String cancelReason;
}

