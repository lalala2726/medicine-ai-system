package com.zhangyichuang.medicine.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Chuang
 * <p>
 * created on 2025/11/8
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderTimelineDto {

    /**
     * 订单ID
     */
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    /**
     * 事件类型
     */
    @NotBlank(message = "事件类型不能为空")
    private String eventType;

    /**
     * 事件状态
     */
    @NotBlank(message = "事件状态不能为空")
    private String eventStatus;

    /**
     * 操作方(USER/ADMIN/SYSTEM)
     */
    @NotBlank(message = "操作方不能为空")
    private String operatorType;

    /**
     * 事件描述
     */
    @NotBlank(message = "事件描述不能为空")
    private String description;
}
