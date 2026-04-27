package com.zhangyichuang.medicine.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author Chuang
 * <p>
 * created on 2026/1/31
 */
@Data
@Schema(description = "药品信息")
public class MedicineInfoDto {

    @Schema(description = "药品通用名", example = "喉咙清颗粒")
    private String commonName;

    @Schema(description = "成分", example = "土牛膝、马兰草、车前草、天名精")
    private String composition;

    @Schema(description = "性状", example = "棕褐色的颗粒；味甜、微苦")
    private String characteristics;

    @Schema(description = "包装规格", example = "复合膜包装，12袋/盒")
    private String packaging;

    @Schema(description = "有效期", example = "24个月")
    private String validityPeriod;

    @Schema(description = "贮藏条件", example = "密封，置阴凉处（不超过20°C）")
    private String storageConditions;

    @Schema(description = "生产单位", example = "湖南时代阳光药业股份有限公司")
    private String productionUnit;

    @Schema(description = "批准文号", example = "国药准字Z20090802")
    private String approvalNumber;

    @Schema(description = "执行标准", example = "国家食品药品监督管理局标准YBZ13322009")
    private String executiveStandard;

    @Schema(description = "产地类型", example = "国产")
    private String originType;

    @Schema(description = "是否外用药", example = "false")
    private Boolean isOutpatientMedicine;

    @Schema(description = "温馨提示", example = "请在医生指导下使用")
    private String warmTips;

    @Schema(description = "品牌名称", example = "时代阳光")
    private String brand;

    @Schema(description = "药品分类编码（0-OTC绿，1-Rx，2-OTC红）", example = "0")
    private Integer drugCategory;

    @Schema(description = "功能主治", example = "清热解毒，利咽止痛")
    private String efficacy;

    @Schema(description = "用法用量", example = "开水冲服，一次1袋，一日3次")
    private String usageMethod;

    @Schema(description = "不良反应", example = "尚不明确")
    private String adverseReactions;

    @Schema(description = "注意事项", example = "孕妇慎用")
    private String precautions;

    @Schema(description = "禁忌", example = "对本品过敏者禁用")
    private String taboo;

    @Schema(description = "药品说明书全文")
    private String instruction;
}
