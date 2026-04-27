package com.zhangyichuang.medicine.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 订单地址信息VO
 *
 * @author Chuang
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "订单地址信息")
public class OrderAddressVo {

    @Schema(description = "订单ID", example = "1")
    private Long orderId;

    @Schema(description = "订单号", example = "ORD20230405001")
    private String orderNo;

    @Schema(description = "订单状态", example = "已发货")
    private String orderStatus;

    @Schema(description = "收货人姓名", example = "张三")
    private String receiverName;

    @Schema(description = "收货人电话", example = "13800138000")
    private String receiverPhone;

    @Schema(description = "收货详细地址", example = "北京市朝阳区某某街道某某小区1号楼101室")
    private String receiverDetail;

    @Schema(description = "配送方式", example = "快递配送")
    private String deliveryType;
}

