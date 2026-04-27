package com.zhangyichuang.medicine.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * @author Chuang
 * <p>
 * created on 2025/11/8
 */
@Data
@Schema(description = "订单时间线视图对象")
public class MallOrderTimelineVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键", example = "1")
    private Long id;

    @Schema(description = "订单ID", example = "1")
    private Long orderId;

    @Schema(description = "事件类型", example = "CREATE")
    private String eventType;

    @Schema(description = "事件状态", example = "SUCCESS")
    private String eventStatus;

    @Schema(description = "操作方(USER/ADMIN/SYSTEM)", example = "USER")
    private String operatorType;

    @Schema(description = "事件描述", example = "订单创建成功")
    private String description;

    @Schema(description = "事件时间", example = "2025-11-08 08:12:00")
    private Date createdTime;
}
