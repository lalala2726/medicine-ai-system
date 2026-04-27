package com.zhangyichuang.medicine.shared.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "地址区域视图对象")
public class RegionVo {

    @Schema(description = "行政区划唯一编码, 国家标准12位或15位行政代码", example = "110000000000")
    private String id;

    @Schema(description = "上级行政区划编码, 省级为\"0\",市级为省id,区县级为市id,街道为区id", example = "0")
    private String parentId;

    @Schema(description = "行政区划中文名称, 例如: \"北京市\"、\"朝阳区\"、\"东花市街道\"", example = "北京市")
    private String name;

    @Schema(description = "行政区划层级, 1=省级, 2=市级, 3=区县级, 5=街道/村级", example = "1")
    private Integer level;
}
