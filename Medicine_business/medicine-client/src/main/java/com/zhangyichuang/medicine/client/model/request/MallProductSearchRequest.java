package com.zhangyichuang.medicine.client.model.request;

import com.zhangyichuang.medicine.common.core.base.PageRequest;
import com.zhangyichuang.medicine.model.dto.MallProductTagFilterGroup;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品搜索请求参数。
 *
 * @author Chuang
 * created on 2025/11/29
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "商品搜索请求参数")
public class MallProductSearchRequest extends PageRequest {

    /**
     * 搜索关键字。
     */
    @Schema(description = "搜索关键字,这个关键字匹配商品名称、商品分类名称、商品描述、厂商名称、药品通用名、功效/主治", example = "感冒灵")
    private String keyword;

    /**
     * 商品名称。
     */
    @Schema(description = "商品名称", example = "999感冒灵颗粒")
    private String name;

    /**
     * 商品分类名称。
     */
    @Schema(description = "商品分类名称", example = "感冒药")
    private String categoryName;

    /**
     * 商品分类ID。
     */
    @Schema(description = "商品分类ID", example = "1")
    private Long categoryId;

    /**
     * 商品价格。
     */
    @Schema(description = "商品价格", example = "29.90")
    private BigDecimal price;

    /**
     * 最低价格。
     */
    @Schema(description = "最低价格", example = "10.00")
    private BigDecimal minPrice;

    /**
     * 最高价格。
     */
    @Schema(description = "最高价格", example = "99.00")
    private BigDecimal maxPrice;

    /**
     * 价格排序方向。
     */
    @Schema(description = "价格排序方向（asc/desc）", example = "asc")
    private String priceSort;

    /**
     * 销量排序方向。
     */
    @Schema(description = "销量排序方向（asc/desc）", example = "desc")
    private String salesSort;

    /**
     * 商品状态。
     */
    @Schema(description = "商品状态", example = "1")
    private Integer status;

    /**
     * 厂商名称。
     */
    @Schema(description = "厂商名称", example = "华润三九医药股份有限公司")
    private String brand;

    /**
     * 药品通用名。
     */
    @Schema(description = "药品通用名", example = "复方氨酚烷胺片")
    private String commonName;

    /**
     * 功效/主治。
     */
    @Schema(description = "功效/主治", example = "用于缓解普通感冒及流行性感冒引起的发热、头痛、四肢酸痛、打喷嚏、流鼻涕、鼻塞、咽痛等症状")
    private String efficacy;

    /**
     * 商品标签ID列表。
     */
    @Schema(description = "商品标签ID列表", example = "[1,2,3]")
    private List<Long> tagIds;

    /**
     * 按标签类型拆分后的筛选条件。
     */
    @Schema(hidden = true)
    private List<MallProductTagFilterGroup> tagFilterGroups;

    /**
     * 构造商品搜索请求。
     */
    public MallProductSearchRequest() {
    }

    /**
     * 构造带关键字和数量限制的商品搜索请求。
     *
     * @param keyword 搜索关键字
     * @param limit   每页数量
     */
    public MallProductSearchRequest(String keyword, int limit) {
        this.keyword = keyword;
        super.setPageSize(limit);
    }
}
