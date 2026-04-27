package com.zhangyichuang.medicine.agent.model.vo.admin;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.zhangyichuang.medicine.agent.annotation.AgentCodeLabel;
import com.zhangyichuang.medicine.agent.annotation.FieldDescription;
import com.zhangyichuang.medicine.agent.mapping.AgentCodeLabelRegistry;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 管理端智能体售后列表视图。
 */
@Schema(description = "管理端智能体售后列表")
@FieldDescription(description = "管理端智能体售后列表")
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AgentAfterSaleListVo {

    @Schema(description = "售后申请ID", example = "1")
    @FieldDescription(description = "售后申请ID")
    private Long id;

    @Schema(description = "售后单号", example = "AS20251108001")
    @FieldDescription(description = "售后单号")
    private String afterSaleNo;

    @Schema(description = "订单编号", example = "O20251108001")
    @FieldDescription(description = "订单编号")
    private String orderNo;

    @Schema(description = "用户ID", example = "1001")
    @FieldDescription(description = "用户ID")
    private Long userId;

    @Schema(description = "用户昵称", example = "张三")
    @FieldDescription(description = "用户昵称")
    private String userNickname;

    @Schema(description = "商品名称", example = "感冒药")
    @FieldDescription(description = "商品名称")
    private String productName;

    @Schema(description = "商品图片", example = "https://example.com/image.jpg")
    @FieldDescription(description = "商品图片")
    private String productImage;

    @Schema(description = "售后类型编码")
    @FieldDescription(description = "售后类型编码")
    @AgentCodeLabel(dictKey = AgentCodeLabelRegistry.AGENT_AFTER_SALE_TYPE)
    private String afterSaleType;

    @Schema(description = "售后状态编码")
    @FieldDescription(description = "售后状态编码")
    @AgentCodeLabel(dictKey = AgentCodeLabelRegistry.AGENT_AFTER_SALE_STATUS)
    private String afterSaleStatus;

    @Schema(description = "退款金额", example = "99.99")
    @FieldDescription(description = "退款金额")
    private BigDecimal refundAmount;

    @Schema(description = "申请原因编码")
    @FieldDescription(description = "申请原因编码")
    @AgentCodeLabel(dictKey = AgentCodeLabelRegistry.AGENT_AFTER_SALE_REASON)
    private String applyReason;

    @Schema(description = "申请时间", example = "2025-11-08 10:00:00")
    @FieldDescription(description = "申请时间")
    private Date applyTime;

    @Schema(description = "审核时间", example = "2025-11-08 15:30:00")
    @FieldDescription(description = "审核时间")
    private Date auditTime;
}
