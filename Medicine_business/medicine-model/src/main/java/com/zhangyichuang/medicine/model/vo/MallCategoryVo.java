package com.zhangyichuang.medicine.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 商城商品分类详情视图对象
 *
 * @author Chuang
 * created on 2025/10/4
 */
@Data
@Schema(description = "商城商品分类详情视图对象")
public class MallCategoryVo {

    @Schema(description = "分类ID", example = "1")
    private Long id;

    @Schema(description = "分类名称", example = "保健品")
    private String name;

    @Schema(description = "父分类ID，0表示顶级分类", example = "0")
    private Long parentId;

    @Schema(description = "分类描述", type = "string", example = "保健品类分类")
    private String description;

    @Schema(description = "封面", example = "https://example.com/category-cover.png")
    private String cover;

    @Schema(description = "排序值，越小越靠前", example = "1")
    private Integer sort;

    @Schema(description = "状态（0-启用，1-禁用）", example = "0")
    private Integer status;

    @Schema(description = "创建时间", example = "2025-01-01 00:00:00")
    private Date createTime;

    @Schema(description = "更新时间", example = "2025-01-01 00:00:00")
    private Date updateTime;

    @Schema(description = "创建者", example = "admin")
    private String createBy;

    @Schema(description = "更新者", example = "admin")
    private String updateBy;

}
