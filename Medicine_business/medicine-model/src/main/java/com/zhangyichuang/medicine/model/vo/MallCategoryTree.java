package com.zhangyichuang.medicine.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * @author Chuang
 * <p>
 * created on 2025/10/4
 */
@Data
@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
public class MallCategoryTree {

    @Schema(description = "ID", example = "1")
    private Long id;

    @Schema(description = "父ID", example = "1")
    private Long parentId;

    @Schema(description = "分类名称", example = "分类名称")
    private String name;

    @Schema(description = "分类描述", example = "分类描述")
    private String description;

    @Schema(description = "封面", example = "https://example.com/category-cover.png")
    private String cover;

    @Schema(description = "排序", example = "1")
    private Integer sort;

    @Schema(description = "状态", example = "1")
    private Integer status;

    @Schema(description = "子分类", example = "{}")
    private List<MallCategoryTree> children;
}
