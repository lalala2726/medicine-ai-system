package com.zhangyichuang.medicine.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 大模型提供商连通性测试结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "大模型提供商连通性测试结果")
public class LlmProviderConnectivityTestVo {

    @Schema(description = "是否连通成功", example = "true")
    private Boolean success;

    @Schema(description = "HTTP 状态码，网络失败时为空", example = "200")
    private Integer httpStatus;

    @Schema(description = "实际请求地址", example = "https://api.openai.com/v1/models")
    private String endpoint;

    @Schema(description = "请求耗时，单位毫秒", example = "312")
    private Long latencyMs;

    @Schema(description = "测试结果说明", example = "连通成功")
    private String message;
}
