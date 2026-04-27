package com.zhangyichuang.medicine.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 预设大模型厂商详情。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "预设大模型厂商详情")
public class LlmPresetProviderDetailVo extends LlmPresetProviderVo {

    @Schema(description = "支持的模型列表")
    private List<LlmPresetProviderModelVo> models;
}
