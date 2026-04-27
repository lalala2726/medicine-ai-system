package com.zhangyichuang.medicine.agent.model.vo.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.zhangyichuang.medicine.agent.annotation.FieldDescription;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 客户端智能体商品规格属性。
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "客户端智能体商品规格属性")
@FieldDescription(description = "客户端智能体商品规格属性")
public class ClientAgentProductSpecVo {

    /**
     * 商品ID。
     */
    @Schema(description = "商品ID")
    @FieldDescription(description = "商品ID")
    private Long productId;

    /**
     * 商品名称。
     */
    @Schema(description = "商品名称")
    @FieldDescription(description = "商品名称")
    private String productName;

    /**
     * 商品分类名称。
     */
    @Schema(description = "商品分类名称")
    @FieldDescription(description = "商品分类名称")
    private String categoryName;

    /**
     * 商品单位。
     */
    @Schema(description = "商品单位")
    @FieldDescription(description = "商品单位")
    private String unit;

    /**
     * 药品通用名。
     */
    @Schema(description = "药品通用名")
    @FieldDescription(description = "药品通用名")
    private String commonName;

    /**
     * 成分。
     */
    @Schema(description = "成分")
    @FieldDescription(description = "成分")
    private String composition;

    /**
     * 性状。
     */
    @Schema(description = "性状")
    @FieldDescription(description = "性状")
    private String characteristics;

    /**
     * 包装规格。
     */
    @Schema(description = "包装规格")
    @FieldDescription(description = "包装规格")
    private String packaging;

    /**
     * 有效期。
     */
    @Schema(description = "有效期")
    @FieldDescription(description = "有效期")
    private String validityPeriod;

    /**
     * 贮藏条件。
     */
    @Schema(description = "贮藏条件")
    @FieldDescription(description = "贮藏条件")
    private String storageConditions;

    /**
     * 生产单位。
     */
    @Schema(description = "生产单位")
    @FieldDescription(description = "生产单位")
    private String productionUnit;

    /**
     * 批准文号。
     */
    @Schema(description = "批准文号")
    @FieldDescription(description = "批准文号")
    private String approvalNumber;

    /**
     * 执行标准。
     */
    @Schema(description = "执行标准")
    @FieldDescription(description = "执行标准")
    private String executiveStandard;

    /**
     * 产地类型。
     */
    @Schema(description = "产地类型")
    @FieldDescription(description = "产地类型")
    private String originType;

    /**
     * 品牌。
     */
    @Schema(description = "品牌")
    @FieldDescription(description = "品牌")
    private String brand;

    /**
     * 药品分类编码（0-OTC绿，1-Rx，2-OTC红）。
     */
    @Schema(description = "药品分类编码（0-OTC绿，1-Rx，2-OTC红）")
    @FieldDescription(description = "药品分类编码（0-OTC绿，1-Rx，2-OTC红）")
    private Integer drugCategory;

    /**
     * 功能主治。
     */
    @Schema(description = "功能主治")
    @FieldDescription(description = "功能主治")
    private String efficacy;

    /**
     * 用法用量。
     */
    @Schema(description = "用法用量")
    @FieldDescription(description = "用法用量")
    private String usageMethod;

    /**
     * 不良反应。
     */
    @Schema(description = "不良反应")
    @FieldDescription(description = "不良反应")
    private String adverseReactions;

    /**
     * 注意事项。
     */
    @Schema(description = "注意事项")
    @FieldDescription(description = "注意事项")
    private String precautions;

    /**
     * 禁忌信息。
     */
    @Schema(description = "禁忌信息")
    @FieldDescription(description = "禁忌信息")
    private String taboo;

    /**
     * 药品说明书全文。
     */
    @Schema(description = "药品说明书全文")
    @FieldDescription(description = "药品说明书全文")
    private String instruction;
}
