package com.zhangyichuang.medicine.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 客户端智能体统一药品详情 DTO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "客户端智能体统一药品详情")
public class ClientAgentProductDetailDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 商品ID。
     */
    @Schema(description = "商品ID", example = "1")
    private Long productId;

    /**
     * 商品名称。
     */
    @Schema(description = "商品名称", example = "999感冒灵颗粒")
    private String productName;

    /**
     * 商品分类名称列表。
     */
    @Schema(description = "商品分类名称列表", example = "[\"感冒用药\",\"中成药\"]")
    private List<String> categoryNames;

    /**
     * 商品单位。
     */
    @Schema(description = "商品单位", example = "盒")
    private String unit;

    /**
     * 商品价格。
     */
    @Schema(description = "商品价格", example = "29.90")
    private BigDecimal price;

    /**
     * 商品库存。
     */
    @Schema(description = "商品库存", example = "100")
    private Integer stock;

    /**
     * 商品销量。
     */
    @Schema(description = "商品销量", example = "256")
    private Integer sales;

    /**
     * 配送方式编码。
     */
    @Schema(description = "配送方式编码", example = "2")
    private Integer deliveryType;

    /**
     * 商品状态编码。
     */
    @Schema(description = "商品状态编码", example = "1")
    private Integer status;

    /**
     * 药品通用名。
     */
    @Schema(description = "药品通用名", example = "感冒灵颗粒")
    private String commonName;

    /**
     * 药品品牌。
     */
    @Schema(description = "药品品牌", example = "999")
    private String brand;

    /**
     * 药品分类编码（0-OTC绿，1-Rx，2-OTC红）。
     */
    @Schema(description = "药品分类编码（0-OTC绿，1-Rx，2-OTC红）", example = "0")
    private Integer drugCategory;

    /**
     * 商品标签名称列表。
     */
    @Schema(description = "商品标签名称列表", example = "[\"退烧\",\"止咳\"]")
    private List<String> tagNames;

    /**
     * 功能主治。
     */
    @Schema(description = "功能主治", example = "解热镇痛")
    private String efficacy;

    /**
     * 禁忌信息。
     */
    @Schema(description = "禁忌信息", example = "严重肝肾功能不全者慎用")
    private String taboo;

    /**
     * 注意事项。
     */
    @Schema(description = "注意事项", example = "服药期间避免饮酒")
    private String precautions;

    /**
     * 用法用量。
     */
    @Schema(description = "用法用量", example = "口服，一次1袋，一日3次")
    private String usageMethod;

    /**
     * 不良反应。
     */
    @Schema(description = "不良反应", example = "偶见皮疹")
    private String adverseReactions;

    /**
     * 温馨提示。
     */
    @Schema(description = "温馨提示", example = "请仔细阅读说明书并按说明使用或在药师指导下购买和使用")
    private String warmTips;

    /**
     * 是否外用药。
     */
    @Schema(description = "是否外用药", example = "false")
    private Boolean isOutpatientMedicine;

    /**
     * 成分。
     */
    @Schema(description = "成分", example = "对乙酰氨基酚、咖啡因")
    private String composition;

    /**
     * 性状。
     */
    @Schema(description = "性状", example = "本品为浅棕色颗粒")
    private String characteristics;

    /**
     * 包装规格。
     */
    @Schema(description = "包装规格", example = "10袋/盒")
    private String packaging;

    /**
     * 有效期。
     */
    @Schema(description = "有效期", example = "24个月")
    private String validityPeriod;

    /**
     * 贮藏条件。
     */
    @Schema(description = "贮藏条件", example = "密封保存")
    private String storageConditions;

    /**
     * 生产单位。
     */
    @Schema(description = "生产单位", example = "华润三九医药股份有限公司")
    private String productionUnit;

    /**
     * 批准文号。
     */
    @Schema(description = "批准文号", example = "国药准字Z44021940")
    private String approvalNumber;

    /**
     * 执行标准。
     */
    @Schema(description = "执行标准", example = "WS3-B-3425-98")
    private String executiveStandard;

    /**
     * 产地类型。
     */
    @Schema(description = "产地类型", example = "国产")
    private String originType;

    /**
     * 药品说明书全文。
     */
    @Schema(description = "药品说明书全文", example = "完整说明书内容")
    private String instruction;
}
