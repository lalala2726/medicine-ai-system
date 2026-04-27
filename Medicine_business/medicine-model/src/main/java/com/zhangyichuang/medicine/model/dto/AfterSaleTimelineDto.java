package com.zhangyichuang.medicine.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 售后时间线传输对象。
 *
 * @author Chuang
 * created 2026/02/28
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "售后时间线DTO")
public class AfterSaleTimelineDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "时间线ID", example = "1")
    private Long id;

    @Schema(description = "事件类型编码", example = "REFUND_APPLY")
    private String eventType;

    @Schema(description = "事件类型名称", example = "退款申请")
    private String eventTypeName;

    @Schema(description = "事件状态编码", example = "PENDING")
    private String eventStatus;

    @Schema(description = "操作人类型编码", example = "USER")
    private String operatorType;

    @Schema(description = "操作人类型名称", example = "用户")
    private String operatorTypeName;

    @Schema(description = "事件描述", example = "用户申请退款")
    private String description;

    @Schema(description = "创建时间", example = "2025-11-08 10:00:00")
    private Date createTime;
}
