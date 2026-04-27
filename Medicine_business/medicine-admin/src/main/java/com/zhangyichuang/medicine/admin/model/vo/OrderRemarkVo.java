package com.zhangyichuang.medicine.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 订单备注信息VO
 *
 * @author Chuang
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "订单备注信息")
public class OrderRemarkVo {
    @Schema(description = "订单ID", example = "1")
    private Long orderId;

    @Schema(description = "订单号", example = "ORDER202307190001")
    private String orderNo;

    @Schema(description = "订单备注", example = "请尽快发货")
    private String remark;

    @Schema(description = "用户留言", example = "希望包装严实一点")
    private String note;
}

