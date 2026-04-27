package com.zhangyichuang.medicine.agent.model.vo.admin;

import com.zhangyichuang.medicine.agent.annotation.FieldDescription;
import com.zhangyichuang.medicine.model.dto.DrugDetailDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 管理端智能体药品详情。
 */
@Schema(description = "管理端智能体药品详情")
@FieldDescription(description = "管理端智能体药品详情")
@Data
public class AgentDrugDetailVo {

    @Schema(description = "商品ID", example = "1")
    @FieldDescription(description = "商品ID")
    private Long productId;

    @Schema(description = "商品名称", example = "维生素C片")
    @FieldDescription(description = "商品名称")
    private String productName;

    @Schema(description = "药品说明信息")
    @FieldDescription(description = "药品说明信息")
    private DrugDetailDto drugDetail;
}
