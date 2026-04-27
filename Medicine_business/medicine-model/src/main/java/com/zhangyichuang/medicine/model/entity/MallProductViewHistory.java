package com.zhangyichuang.medicine.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 商品浏览历史与浏览量统计
 */
@TableName(value = "mall_product_view_history")
@Data
public class MallProductViewHistory {

    /**
     * 自增主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 该用户对该商品的累计浏览次数
     */
    private Integer viewCount;

    /**
     * 该用户最后一次浏览该商品的时间
     */
    private Date lastViewTime;

    /**
     * 第一次浏览时间
     */
    private Date firstViewTime;
}
