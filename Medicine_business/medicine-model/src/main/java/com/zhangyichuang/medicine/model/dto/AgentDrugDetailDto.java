package com.zhangyichuang.medicine.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 管理端智能体药品详情。
 */
@Data
public class AgentDrugDetailDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "产品ID", example = "1234567890")
    private Long productId;

    @Schema(description = "产品名称", example = "板蓝根颗粒")
    private String productName;

    @Schema(description = "药品详情")
    private DrugDetailDto drugDetail;
}
