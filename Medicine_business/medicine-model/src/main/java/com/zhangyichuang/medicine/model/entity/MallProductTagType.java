package com.zhangyichuang.medicine.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 商品标签类型实体。
 *
 * @author Chuang
 */
@Data
@TableName("mall_product_tag_type")
public class MallProductTagType {

    /**
     * 标签类型ID。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 标签类型编码。
     */
    private String code;

    /**
     * 标签类型名称。
     */
    private String name;

    /**
     * 排序值，越小越靠前。
     */
    private Integer sort;

    /**
     * 状态：1-启用，0-禁用。
     */
    private Integer status;

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
