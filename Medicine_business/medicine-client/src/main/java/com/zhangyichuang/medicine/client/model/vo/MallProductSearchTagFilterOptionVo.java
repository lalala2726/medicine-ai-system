package com.zhangyichuang.medicine.client.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 商品搜索标签筛选项视图对象。
 *
 * @author Chuang
 */
@Data
@Schema(description = "商品搜索标签筛选项视图对象")
public class MallProductSearchTagFilterOptionVo {

    /**
     * 标签ID。
     */
    @Schema(description = "标签ID", example = "1")
    private Long tagId;

    /**
     * 标签名称。
     */
    @Schema(description = "标签名称", example = "退烧")
    private String tagName;

    /**
     * 当前筛选命中数量。
     */
    @Schema(description = "当前筛选命中数量", example = "12")
    private Long count;
}
