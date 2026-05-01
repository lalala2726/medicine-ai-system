package com.zhangyichuang.medicine.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * Agent Trace 耗时趋势时间点视图对象。
 */
@Data
@Schema(description = "Agent Trace耗时趋势时间点视图对象")
public class AgentTraceMonitorDurationTrendPointVo {

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
     * 平均耗时毫秒。
     */
    @Schema(description = "平均耗时毫秒")
    private Long avgDurationMs;

    /**
     * 最大耗时毫秒。
     */
    @Schema(description = "最大耗时毫秒")
    private Long maxDurationMs;
}
