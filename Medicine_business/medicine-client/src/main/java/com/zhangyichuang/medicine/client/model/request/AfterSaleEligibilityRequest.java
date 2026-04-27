package com.zhangyichuang.medicine.client.model.request;

import com.zhangyichuang.medicine.model.enums.AfterSaleScopeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * 用户端售后资格校验请求。
 */
@Data
@Schema(description = "用户端售后资格校验请求")
public class AfterSaleEligibilityRequest {

    @NotBlank(message = "订单编号不能为空")
    @Schema(description = "订单编号", example = "O202511130001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String orderNo;

    @Schema(description = "申请范围，不传默认按整单校验", example = "ORDER")
    private AfterSaleScopeEnum scope;

    @Positive(message = "订单项ID必须为正数")
    @Schema(description = "订单项ID，scope=ITEM 时可传", example = "1")
    private Long orderItemId;
}
