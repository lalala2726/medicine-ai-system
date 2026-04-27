package com.zhangyichuang.medicine.agent.model.vo.client;

import com.zhangyichuang.medicine.agent.annotation.FieldDescription;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 客户端智能体商品搜索标签筛选分组。
 */
@Data
@Schema(description = "客户端智能体商品搜索标签筛选分组")
@FieldDescription(description = "客户端智能体商品搜索标签筛选分组")
public class ClientAgentProductSearchTagFilterVo {

    /**
     * 标签类型ID。
     */
    @Schema(description = "标签类型ID", example = "1")
    @FieldDescription(description = "标签类型ID")
    private Long typeId;

    /**
     * 标签类型编码。
     */
    @Schema(description = "标签类型编码", example = "EFFICACY")
    @FieldDescription(description = "标签类型编码")
    private String typeCode;

    /**
     * 标签类型名称。
     */
    @Schema(description = "标签类型名称", example = "功效")
    @FieldDescription(description = "标签类型名称")
    private String typeName;

    /**
     * 当前类型下的标签项列表。
     */
    @Schema(description = "当前类型下的标签项列表")
    @FieldDescription(description = "当前类型下的标签项列表")
    private List<ClientAgentProductSearchTagFilterOptionVo> options;
}
