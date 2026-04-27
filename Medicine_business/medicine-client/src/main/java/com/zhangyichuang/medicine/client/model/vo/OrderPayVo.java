package com.zhangyichuang.medicine.client.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单支付响应对象
 * <p>
 * 根据支付方式返回不同的数据结构:
 * - 钱包支付: 返回订单信息 + 支付成功标识
 * - 钱包支付: 返回订单信息 + 同步支付结果
 * </p>
 *
 * @author Chuang
 * created on 2025/11/13
 */
@Data
@Builder
@Schema(description = "订单支付响应对象")
public class OrderPayVo {

    @Schema(description = "订单号", example = "O20251113112233445566")
    private String orderNo;

    @Schema(description = "支付金额", example = "128.50")
    private BigDecimal payAmount;

    @Schema(description = "订单状态", example = "PENDING_SHIPMENT")
    private String orderStatus;

    @Schema(description = "支付方式", example = "WALLET")
    private String paymentMethod;

    @Schema(description = "支付状态: SUCCESS-支付成功, PENDING-待支付", example = "SUCCESS")
    private String paymentStatus;

    @Schema(description = "支付数据: 钱包支付固定为null", example = "null")
    private String paymentData;
}
