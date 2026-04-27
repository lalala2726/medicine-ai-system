package com.zhangyichuang.medicine.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author Chuang
 * <p>
 * created on 2025/11/7
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "用户消费信息")
public class UserConsumeInfo {
    @Schema(description = "索引", example = "1")
    private Long index;

    @Schema(description = "用户ID", example = "1001")
    private Long userId;

    @Schema(description = "订单编号", example = "ORD20251107001")
    private String orderNo;

    @Schema(description = "商品总价", example = "99.99")
    private BigDecimal totalPrice;

    @Schema(description = "实付金额", example = "89.99")
    private BigDecimal payPrice;

    @Schema(description = "完成时间", example = "2025-11-07 15:30:00")
    private Date finishTime;


}
