package com.zhangyichuang.medicine.model.elasticsearch.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;
import org.springframework.data.elasticsearch.core.suggest.Completion;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品搜索索引文档。
 *
 * @author Chuang
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = MallProductDocument.INDEX_NAME, createIndex = true)
@Setting(settingPath = "/elasticsearch/mall_product-settings.json")
public class MallProductDocument {

    /**
     * 商品索引名称。
     */
    public static final String INDEX_NAME = "mall_product";

    /**
     * 商品ID。
     */
    @Id
    private Long id;

    /**
     * 商品名称。
     */
    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "ik_pinyin_index", searchAnalyzer = "ik_pinyin_search"),
            otherFields = @InnerField(suffix = "keyword", type = FieldType.Keyword)
    )
    private String name;

    /**
     * 商品分类名称列表。
     */
    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "ik_pinyin_index", searchAnalyzer = "ik_pinyin_search"),
            otherFields = @InnerField(suffix = "keyword", type = FieldType.Keyword)
    )
    private List<String> categoryNames;

    /**
     * 商品分类ID列表。
     */
    @Field(type = FieldType.Long)
    private List<Long> categoryIds;

    /**
     * 药品分类编码（0-OTC绿，1-Rx，2-OTC红）。
     */
    @Field(type = FieldType.Integer)
    private Integer drugCategory;

    /**
     * 商品价格。
     */
    @Field(type = FieldType.Scaled_Float, scalingFactor = 100)
    private BigDecimal price;

    /**
     * 商品销量。
     */
    @Field(type = FieldType.Integer)
    private Integer sales;

    /**
     * 商品状态。
     */
    @Field(type = FieldType.Integer)
    private Integer status;

    /**
     * 药品品牌。
     */
    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "ik_pinyin_index", searchAnalyzer = "ik_pinyin_search"),
            otherFields = @InnerField(suffix = "keyword", type = FieldType.Keyword)
    )
    private String brand;

    /**
     * 药品通用名。
     */
    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "ik_pinyin_index", searchAnalyzer = "ik_pinyin_search"),
            otherFields = @InnerField(suffix = "keyword", type = FieldType.Keyword)
    )
    private String commonName;

    /**
     * 商品关键字补全。
     */
    @CompletionField(analyzer = "ik_pinyin_index", searchAnalyzer = "ik_pinyin_search", maxInputLength = 100)
    private Completion keywordSuggest;

    /**
     * 药品功效。
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String efficacy;

    /**
     * 商品标签ID列表。
     */
    @Field(type = FieldType.Long)
    private List<Long> tagIds;

    /**
     * 商品标签名称列表。
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private List<String> tagNames;

    /**
     * 标签类型绑定列表。
     */
    @Field(type = FieldType.Keyword)
    private List<String> tagTypeBindings;

    /**
     * 商品主图。
     */
    @Field(type = FieldType.Keyword, index = false)
    private String coverImage;

    /**
     * 药品说明书。
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String instruction;

    /**
     * 药品禁忌信息。
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String taboo;
}
