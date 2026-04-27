package com.zhangyichuang.medicine.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 药品详细信息表
 */
@TableName(value = "drug_detail")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DrugDetail {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联商城商品ID（mall_product.id）
     */
    private Long productId;

    /**
     * 药品通用名，例如“喉咙清颗粒”
     */
    private String commonName;

    /**
     * 成分（如“土牛膝、马兰草、车前草、天名精…”）
     */
    private String composition;

    /**
     * 性状（如“棕褐色的颗粒；味甜、微苦…”）
     */
    private String characteristics;

    /**
     * 包装规格（如“复合膜包装，12袋/盒”）
     */
    private String packaging;

    /**
     * 有效期（如“24个月”）
     */
    private String validityPeriod;

    /**
     * 贮藏条件（如“密封，置阴凉处（不超过20°C）”）
     */
    private String storageConditions;

    /**
     * 生产单位（如“湖南时代阳光药业股份有限公司”）
     */
    private String productionUnit;

    /**
     * 批准文号（如“国药准字Z20090802”）
     */
    private String approvalNumber;

    /**
     * 执行标准（如“国家食品药品监督管理局标准YBZ13322009”）
     */
    private String executiveStandard;

    /**
     * 产地类型（如“国产”或“进口”）
     */
    private String originType;

    /**
     * 是否外用药（如“否”）
     */
    private Boolean isOutpatientMedicine;

    /**
     * 温馨提示
     */
    private String warmTips;

    /**
     * 品牌名称
     */
    private String brand;

    /**
     * 药品分类编码（0-OTC绿，1-Rx，2-OTC红）
     */
    @TableField("drug_category")
    private Integer drugCategory;

    /**
     * 功能主治
     */
    private String efficacy;

    /**
     * 用法用量
     */
    private String usageMethod;

    /**
     * 不良反应
     */
    private String adverseReactions;

    /**
     * 注意事项
     */
    private String precautions;

    /**
     * 禁忌
     */
    private String taboo;

    /**
     * 药品说明书全文（可选）
     */
    private String instruction;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 逻辑删除(0否,1是)
     */
    @TableLogic
    private Integer isDeleted;
}
