package com.zhangyichuang.medicine.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 商品分类关联实体。
 *
 * @author Chuang
 */
@Data
@TableName("mall_product_category_rel")
public class MallProductCategoryRel {

    /**
     * 关联ID。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 商品ID。
     */
    private Long productId;

    /**
     * 分类ID。
     */
    private Long categoryId;

    /**
     * 创建时间。
     */
    private Date createTime;

    /**
     * 更新时间。
     */
    private Date updateTime;

    /**
     * 创建人。
     */
    private String createBy;

    /**
     * 更新人。
     */
    private String updateBy;

    /**
     * 逻辑删除标记：0-否，1-是。
     */
    @TableLogic
    private Integer isDeleted;
}
