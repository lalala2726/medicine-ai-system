package com.zhangyichuang.medicine.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 售后列表视图对象（管理端）。
 * <p>
 * 功能描述：用于管理端售后列表接口响应，字段仅保留前端所需原始码值与基础展示信息。
 * </p>
 *
 * @author Chuang
 * created on 2026/02/28
 */
@Data
@Schema(description = "售后列表")
public class MallAfterSaleListVo {

    @Schema(description = "售后申请ID", example = "1")
    private Long id;

    @Schema(description = "售后单号", example = "AS20251108001")
    private String afterSaleNo;

    @Schema(description = "订单ID", example = "10")
    private Long orderId;

    @Schema(description = "订单编号", example = "O20251108001")
    private String orderNo;

    @Schema(description = "订单项ID", example = "100")
    private Long orderItemId;

    @Schema(description = "用户ID", example = "1001")
    private Long userId;

    @Schema(description = "用户昵称", example = "张三")
    private String userNickname;

    @Schema(description = "商品名称", example = "感冒药")
    private String productName;

    @Schema(description = "商品图片", example = "https://example.com/image.jpg")
    private String productImage;

    @Schema(description = "售后类型原始码值", example = "REFUND_ONLY")
    private String afterSaleType;

    @Schema(description = "售后状态原始码值", example = "PENDING")
    private String afterSaleStatus;

    @Schema(description = "退款金额", example = "99.99")
    private BigDecimal refundAmount;

    @Schema(description = "申请原因原始码值", example = "DAMAGED")
    private String applyReason;

    @Schema(description = "申请时间", example = "2025-11-08 10:00:00")
    private Date applyTime;

    @Schema(description = "审核时间", example = "2025-11-08 15:30:00")
    private Date auditTime;
}
