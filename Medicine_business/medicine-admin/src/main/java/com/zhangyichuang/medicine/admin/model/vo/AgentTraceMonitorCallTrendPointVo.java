package com.zhangyichuang.medicine.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * Agent Trace 调用次数趋势时间点视图对象。
 */
@Data
@Schema(description = "Agent Trace调用次数趋势时间点视图对象")
public class AgentTraceMonitorCallTrendPointVo {

    /**
     * 时间桶开始时间。
     */
    @Schema(description = "时间桶开始时间")
    private Date bucketStart;

    /**
     * 时间桶结束时间。
     */
    @Schema(description = "时间桶结束时间")
    private Date bucketEnd;

    /**
     * 模型调用总次数。
     */
    @Schema(description = "模型调用总次数")
    private Long callCount;

    /**
     * 成功次数。
     */
    @Schema(description = "成功次数")
    private Long successCount;

    /**
     * 失败次数。
     */
    @Schema(description = "失败次数")
    private Long errorCount;
}
