package com.zhangyichuang.medicine.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 商品标签管理视图对象。
 *
 * @author Chuang
 */
@Data
@Schema(description = "商品标签管理视图对象")
public class MallProductTagAdminVo {

    /**
     * 标签ID。
     */
    @Schema(description = "标签ID", example = "1")
    private Long id;

    /**
     * 标签名称。
     */
    @Schema(description = "标签名称", example = "退烧")
    private String name;

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
     * 排序值。
     */
    @Schema(description = "排序值，越小越靠前", example = "1")
    private Integer sort;

    /**
     * 标签状态。
     */
    @Schema(description = "状态：1-启用，0-禁用", example = "1")
    private Integer status;

    /**
     * 创建时间。
     */
    @Schema(description = "创建时间", example = "2026-03-21 12:00:00")
    private Date createTime;

    /**
     * 更新时间。
     */
    @Schema(description = "更新时间", example = "2026-03-21 12:00:00")
    private Date updateTime;

    /**
     * 创建人。
     */
    @Schema(description = "创建人", example = "admin")
    private String createBy;

    /**
     * 更新人。
     */
    @Schema(description = "更新人", example = "admin")
    private String updateBy;
}
