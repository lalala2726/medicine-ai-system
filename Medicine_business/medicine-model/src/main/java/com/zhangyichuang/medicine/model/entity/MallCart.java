package com.zhangyichuang.medicine.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 商城购物车表
 */
@TableName(value = "mall_cart")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MallCart {

    /**
     * 购物车 ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 商品 ID
     */
    private Long productId;

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 商品图片（主图）
     */
    private String productImage;

    /**
     * 购买数量
     */
    private Integer cartNum;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}
