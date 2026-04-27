package com.zhangyichuang.medicine.agent.model.vo.client;

import com.zhangyichuang.medicine.agent.annotation.FieldDescription;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 客户端智能体商品搜索标签筛选项。
 */
@Data
@Schema(description = "客户端智能体商品搜索标签筛选项")
@FieldDescription(description = "客户端智能体商品搜索标签筛选项")
public class ClientAgentProductSearchTagFilterOptionVo {

    /**
     * 标签ID。
     */
    @Schema(description = "标签ID", example = "1")
    @FieldDescription(description = "标签ID")
    private Long tagId;

    /**
     * 标签名称。
     */
    @Schema(description = "标签名称", example = "退烧")
    @FieldDescription(description = "标签名称")
    private String tagName;

    /**
     * 当前筛选命中数量。
     */
    @Schema(description = "当前筛选命中数量", example = "12")
    @FieldDescription(description = "当前筛选命中数量")
    private Long count;
}
