package com.zhangyichuang.medicine.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 商品单位新增请求。
 *
 * @author Chuang
 */
@Data
@Schema(description = "商品单位新增请求")
public class MallProductUnitAddRequest {

    /**
     * 单位名称。
     */
    @NotBlank(message = "商品单位名称不能为空")
    @Schema(description = "单位名称", example = "盒", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;
}
