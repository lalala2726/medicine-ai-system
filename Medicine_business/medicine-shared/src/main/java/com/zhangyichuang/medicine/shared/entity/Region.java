package com.zhangyichuang.medicine.shared.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serial;
import java.io.Serializable;

/**
 * 地址区域实体类
 * 存储省市区街道信息
 *
 * @author Chuang
 */
@Data
@Document(collection = "regions")
public class Region implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * MongoDB文档ID
     */
    @Id
    private String mongoId;

    /**
     * 行政区划唯一编码
     * 国家标准12位或15位行政代码
     */
    @Indexed(name = "idx_regions_id")
    @Field("id")
    private String id;

    /**
     * 上级行政区划编码
     * 省级为"0",市级为省id,区县级为市id,街道为区id
     */
    @Indexed(name = "idx_regions_parent_id")
    @Field("parent_id")
    private String parentId;

    /**
     * 行政区划中文名称
     * 例如: "北京市"、"朝阳区"、"东花市街道"
     */
    @Field("name")
    private String name;

    /**
     * 名称全拼
     * 单词间以空格分隔,例如: "bei jing shi"
     */
    @Field("pinyin")
    private String pinyin;

    /**
     * 拼音首字母
     * 用于快速索引,例如: "B"、"C"
     */
    @Indexed(name = "idx_regions_pinyin_prefix")
    @Field("pinyin_prefix")
    private String pinyinPrefix;

    /**
     * 行政区划层级
     * 1=省级, 2=市级, 3=区县级, 5=街道/村级
     */
    @Indexed(name = "idx_regions_level")
    @Field("level")
    private Integer level;
}
