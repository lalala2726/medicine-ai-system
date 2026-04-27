package com.zhangyichuang.medicine.agent.model.vo.health;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.zhangyichuang.medicine.agent.annotation.FieldDescription;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Agent RPC 健康检查结果。
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Agent RPC 健康检查结果")
@FieldDescription(description = "Agent RPC 健康检查结果")
public class RpcHealthVo {

    @Schema(description = "整体状态", example = "UP")
    @FieldDescription(description = "整体状态")
    private String overallStatus;

    @Schema(description = "管理端 RPC 探测结果")
    @FieldDescription(description = "管理端 RPC 探测结果")
    private DependencyHealthVo admin;

    @Schema(description = "客户端 RPC 探测结果")
    @FieldDescription(description = "客户端 RPC 探测结果")
    private DependencyHealthVo client;

    /**
     * 单个依赖的 RPC 探测结果。
     */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "单个依赖的 RPC 探测结果")
    @FieldDescription(description = "单个依赖的 RPC 探测结果")
    public static class DependencyHealthVo {

        @Schema(description = "是否可达", example = "true")
        @FieldDescription(description = "是否可达")
        private boolean reachable;

        @Schema(description = "依赖状态", example = "UP")
        @FieldDescription(description = "依赖状态")
        private String status;

        @Schema(description = "探测耗时（毫秒）", example = "12")
        @FieldDescription(description = "探测耗时（毫秒）")
        private long latencyMs;

        @Schema(description = "归一化原因", example = "OK")
        @FieldDescription(description = "归一化原因")
        private String reason;
    }
}
