package com.zhangyichuang.medicine.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Elasticsearch 与商品索引概览。
 */
@Data
@Schema(description = "Elasticsearch 与商品索引概览")
public class EsIndexConfigVo {

    /**
     * Elasticsearch 是否可用。
     */
    @Schema(description = "Elasticsearch 是否可用")
    private Boolean esAvailable;

    /**
     * 商品索引名称。
     */
    @Schema(description = "商品索引名称", example = "mall_product")
    private String indexName;

    /**
     * 商品索引是否存在。
     */
    @Schema(description = "商品索引是否存在")
    private Boolean indexExists;

    /**
     * 当前商品索引文档数量。
     */
    @Schema(description = "当前商品索引文档数量", example = "128")
    private Long documentCount;

    /**
     * 是否启用启动自动重建。
     */
    @Schema(description = "是否启用启动自动重建")
    private Boolean startupAutoRebuildEnabled;

    /**
     * 启动自动重建触发策略说明。
     */
    @Schema(description = "启动自动重建触发策略说明")
    private String startupTriggerPolicy;

    /**
     * 商品索引重建运行状态。
     */
    @Schema(description = "商品索引重建运行状态")
    private ProductIndexRebuildStatusVo rebuildStatus;

    /**
     * 商品索引重建运行状态视图对象。
     */
    @Data
    @Schema(description = "商品索引重建运行状态视图对象")
    public static class ProductIndexRebuildStatusVo {

        /**
         * 当前是否正在重建。
         */
        @Schema(description = "当前是否正在重建")
        private Boolean running;

        /**
         * 本次触发来源。
         */
        @Schema(description = "本次触发来源", example = "startup")
        private String triggerSource;

        /**
         * 当前已处理的商品数量。
         */
        @Schema(description = "当前已处理的商品数量", example = "80")
        private Long processedCount;

        /**
         * 本次预计处理的商品总数量。
         */
        @Schema(description = "本次预计处理的商品总数量", example = "200")
        private Long totalCount;

        /**
         * 当前已完成的批次数量。
         */
        @Schema(description = "当前已完成的批次数量", example = "2")
        private Long batchCount;

        /**
         * 本次重建开始时间。
         */
        @Schema(description = "本次重建开始时间")
        private LocalDateTime startedTime;

        /**
         * 最近一次完成时间。
         */
        @Schema(description = "最近一次完成时间")
        private LocalDateTime finishedTime;

        /**
         * 最近一次错误信息。
         */
        @Schema(description = "最近一次错误信息")
        private String lastError;
    }
}
