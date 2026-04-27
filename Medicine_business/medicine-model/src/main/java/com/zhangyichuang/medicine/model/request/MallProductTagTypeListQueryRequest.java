package com.zhangyichuang.medicine.model.request;

import com.zhangyichuang.medicine.common.core.base.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商品标签类型列表查询请求。
 *
 * @author Chuang
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "商品标签类型列表查询请求")
public class MallProductTagTypeListQueryRequest extends PageRequest {

    /**
     * 标签类型名称。
     */
    @Schema(description = "标签类型名称", example = "适用人群")
    private String name;

    /**
     * 标签类型状态。
     */
    @Schema(description = "状态：1-启用，0-禁用", example = "1")
    private Integer status;
}
