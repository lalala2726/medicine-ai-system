package com.zhangyichuang.medicine.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * Agent Trace Token 与缓存图表时间点视图对象。
 */
@Data
@Schema(description = "Agent Trace Token与缓存图表时间点视图对象")
public class AgentTraceMonitorTokenCachePointVo {

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
     * 输入 Token 数。
     */
    @Schema(description = "输入Token数")
    private Long inputTokens;

    /**
     * 输出 Token 数。
     */
    @Schema(description = "输出Token数")
    private Long outputTokens;

    /**
     * 总 Token 数。
     */
    @Schema(description = "总Token数")
    private Long totalTokens;

    /**
     * 缓存命中 Token 数。
     */
    @Schema(description = "缓存命中Token数")
    private Long cacheReadTokens;

    /**
     * 缓存创建 Token 数。
     */
    @Schema(description = "缓存创建Token数")
    private Long cacheWriteTokens;

    /**
     * 缓存总 Token 数。
     */
    @Schema(description = "缓存总Token数")
    private Long cacheTotalTokens;
}
