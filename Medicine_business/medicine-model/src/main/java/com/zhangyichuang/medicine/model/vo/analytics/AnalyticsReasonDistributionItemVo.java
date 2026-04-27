package com.zhangyichuang.medicine.model.vo.analytics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 售后原因分布项。
 */
@Data
@Schema(description = "售后原因分布项")
public class AnalyticsReasonDistributionItemVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "售后原因编码")
    private String reason;

    @Schema(description = "售后原因名称")
    private String reasonName;

    @Schema(description = "数量")
    private Long count;
}
