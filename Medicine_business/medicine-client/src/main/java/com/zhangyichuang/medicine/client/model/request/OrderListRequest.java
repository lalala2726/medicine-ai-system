package com.zhangyichuang.medicine.client.model.request;

import com.zhangyichuang.medicine.common.core.base.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 客户端查询订单列表请求
 *
 * @author Chuang
 * created 2025/11/10
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "查询订单列表请求")
public class OrderListRequest extends PageRequest {

    @Schema(description = "订单状态(PENDING_PAYMENT-待支付, PENDING_SHIPMENT-待发货, PENDING_RECEIPT-待收货, COMPLETED-已完成, CANCELLED-已取消等)", example = "PENDING_PAYMENT")
    private String orderStatus;

    @Schema(description = "订单编号", example = "O20251110123456789012")
    private String orderNo;

    @Schema(description = "商品名称(模糊搜索)", example = "阿莫西林")
    private String productName;
}

