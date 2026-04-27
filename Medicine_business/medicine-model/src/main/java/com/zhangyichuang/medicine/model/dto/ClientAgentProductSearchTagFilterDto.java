package com.zhangyichuang.medicine.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 客户端智能体商品搜索标签筛选分组 DTO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "客户端智能体商品搜索标签筛选分组")
public class ClientAgentProductSearchTagFilterDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 标签类型ID。
     */
    @Schema(description = "标签类型ID", example = "1")
    private Long typeId;

    /**
     * 标签类型编码。
     */
    @Schema(description = "标签类型编码", example = "EFFICACY")
    private String typeCode;

    /**
     * 标签类型名称。
     */
    @Schema(description = "标签类型名称", example = "功效")
    private String typeName;

    /**
     * 当前类型下的标签选项列表。
     */
    @Schema(description = "当前类型下的标签选项列表")
    private List<ClientAgentProductSearchTagFilterOptionDto> options;
}
