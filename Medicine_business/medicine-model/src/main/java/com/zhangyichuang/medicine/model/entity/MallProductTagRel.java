package com.zhangyichuang.medicine.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 商品标签关联实体。
 *
 * @author Chuang
 */
@Data
@TableName("mall_product_tag_rel")
public class MallProductTagRel {

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
     * 标签ID。
     */
    private Long tagId;

    /**
     * 创建时间。
     */
    private Date createTime;

    /**
     * 创建人。
     */
    private String createBy;
}
