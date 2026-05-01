package com.zhangyichuang.medicine.admin.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * Agent Trace 监控查询参数。
 */
@Data
@Schema(description = "Agent Trace监控查询参数")
public class AgentTraceMonitorRequest {

    /**
     * 统计开始时间。
     */
    @Schema(description = "统计开始时间", example = "2026-04-30 00:00:00")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    /**
     * 统计结束时间。
     */
    @Schema(description = "统计结束时间", example = "2026-04-30 23:59:59")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    /**
     * 会话类型。
     */
    @Schema(description = "会话类型：admin/client", example = "client")
    private String conversationType;

    /**
     * 模型供应商。
     */
    @Schema(description = "模型供应商", example = "aliyun")
    private String provider;

    /**
     * 真实模型名称。
     */
    @Schema(description = "真实模型名称", example = "qwen3.5-plus")
    private String modelName;

    /**
     * 模型槽位。
     */
    @Schema(description = "模型槽位", example = "clientAssistant.serviceNodeModel")
    private String slot;

    /**
     * 调用状态。
     */
    @Schema(description = "调用状态：success/error", example = "success")
    private String status;

    /**
     * 时间桶分钟数。
     */
    @Schema(description = "时间桶分钟数", example = "60")
    private Integer bucketMinutes;
}
