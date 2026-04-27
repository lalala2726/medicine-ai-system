package com.zhangyichuang.medicine.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 商城商品分类修改请求对象
 *
 * @author Chuang
 * created on 2025/10/4
 */
@Data
@Schema(description = "商城商品分类修改请求对象")
public class MallCategoryUpdateRequest {

    /**
     * 分类ID
     */
    @NotNull(message = "分类ID不能为空")
    @Schema(description = "分类ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    /**
     * 分类名称
     */
    @NotBlank(message = "分类名称不能为空")
    @Schema(description = "分类名称", example = "保健品", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    /**
     * 父分类ID，0表示顶级分类
     */
    @Schema(description = "父分类ID，0表示顶级分类", example = "0")
    private Long parentId;

    /**
     * 分类描述
     */
    @Schema(description = "分类描述", example = "保健品类分类")
    private String description;

    @NotBlank(message = "封面不能为空")
    @Schema(description = "封面", example = "https://example.com/category-cover.png", requiredMode = Schema.RequiredMode.REQUIRED)
    private String cover;

    /**
     * 排序值，越小越靠前
     */
    @Schema(description = "排序值，越小越靠前", example = "1")
    private Integer sort;

}
