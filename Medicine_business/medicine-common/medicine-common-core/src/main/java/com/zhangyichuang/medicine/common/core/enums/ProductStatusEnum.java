package com.zhangyichuang.medicine.common.core.enums;

import lombok.Getter;

/**
 * 商品状态枚举
 * 用于表示商品在商城中的当前销售状态
 * <p>
 * 销售中：可正常展示和购买
 * 已售罄：库存为 0，不可购买但仍展示
 * 已下架：不再展示，后台仍可编辑
 * 回收站：逻辑删除状态，待彻底删除或恢复
 */
@Getter
public enum ProductStatusEnum {

    /**
     * 销售中
     */
    ON_SALE("ON_SALE", "销售中"),

    /**
     * 已售罄
     */
    SOLD_OUT("SOLD_OUT", "已售罄"),

    /**
     * 已下架
     */
    OFF_SHELF("OFF_SHELF", "已下架"),

    /**
     * 回收站
     */
    RECYCLED("RECYCLED", "回收站");

    /**
     * 状态编码
     */
    private final String code;

    /**
     * 状态描述
     */
    private final String desc;

    ProductStatusEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据 code 获取枚举
     */
    public static ProductStatusEnum fromCode(String code) {
        for (ProductStatusEnum status : values()) {
            if (status.getCode().equalsIgnoreCase(code)) {
                return status;
            }
        }
        return null;
    }
}
