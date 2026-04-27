package com.zhangyichuang.medicine.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 商城商品分类实体类
 * <p>
 * 用于存储商城商品分类信息的数据库实体，支持树形结构的分类管理。
 * 包含分类基本信息、层级关系、排序和状态管理等字段。
 *
 * @author Chuang
 * created on 2025/10/4
 */
@TableName(value = "mall_category")
@Data
public class MallCategory {

    /**
     * 分类 ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 分类名称
     */
    private String name;

    /**
     * 父分类ID，0表示顶级分类
     */
    private Long parentId;

    /**
     * 分类描述
     */
    private String description;

    /**
     * 封面
     */
    private String cover;

    /**
     * 排序值，越小越靠前
     */
    private Integer sort;

    /**
     * 状态（0-启用，1-禁用）
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 更新人
     */
    private String updateBy;
}
