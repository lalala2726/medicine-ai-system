package com.zhangyichuang.medicine.model.vo.analytics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 售后状态分布项。
 */
@Data
@Schema(description = "售后状态分布项")
public class AnalyticsStatusDistributionItemVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "售后状态编码")
    private String status;

    @Schema(description = "售后状态名称")
    private String statusName;

    @Schema(description = "数量")
    private Long count;
}
