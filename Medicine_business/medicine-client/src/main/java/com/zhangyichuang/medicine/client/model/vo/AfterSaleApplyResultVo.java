package com.zhangyichuang.medicine.client.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 用户端售后申请结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户端售后申请结果")
public class AfterSaleApplyResultVo {

    @Schema(description = "订单编号")
    private String orderNo;

    @Schema(description = "请求范围")
    private String requestedScope;

    @Schema(description = "最终生效范围")
    private String resolvedScope;

    @Schema(description = "生成的售后单号列表")
    private List<String> afterSaleNos;

    @Schema(description = "本次创建的订单项ID列表")
    private List<Long> orderItemIds;
}
