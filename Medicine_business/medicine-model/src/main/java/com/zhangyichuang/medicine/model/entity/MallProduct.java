package com.zhangyichuang.medicine.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 商城商品主实体类
 * <p>
 * 用于存储商城商品基本信息的数据库实体，支持与药品库存的绑定。
 * 包含商品基本信息、价格、库存、分类关联、配送方式等核心业务字段。
 *
 * @author Chuang
 * created on 2025/10/4
 */
@TableName(value = "mall_product")
@Data
public class MallProduct implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 商品ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 商品名称
     */
    private String name;

    /**
     * 商品分类ID，关联 mall_category
     */
    private Long categoryId;

    /**
     * 商品单位（件、盒、瓶等）
     */
    private String unit;

    /**
     * 展示价/兜底价：单规格=唯一SKU价，多规格=最小SKU价；结算以SKU价为准
     */
    private BigDecimal price;

    /**
     * 库存数量
     */
    private Integer stock;

    /**
     * 排序值，越小越靠前
     */
    private Integer sort;

    /**
     * 状态（1-上架，0-下架）
     */
    private Integer status;

    /**
     * 配送方式（快递、自提、同城配送等）
     */
    private Integer deliveryType;

    /**
     * 是否允许使用优惠券（1-允许，0-不允许）。
     */
    private Integer couponEnabled;

    /**
     * 版本号，用于乐观锁控制
     */
    private Integer version;

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

    /**
     * 逻辑删除(0否,1是)
     */
    @TableLogic
    private Integer isDeleted;
}
