package com.zhangyichuang.medicine.model.enums;

import lombok.Getter;

/**
 * 配送方式枚举
 *
 * @author Chuang
 * created 2025/10/14
 */
@Getter
public enum DeliveryTypeEnum {

    /**
     * 咨询商家
     */
    CONSULT_SELLER("CONSULT_SELLER", "咨询商家", "用户咨询商家进行咨询"),

    /**
     * 用户自提
     */
    SELF_PICKUP("SELF_PICKUP", "自提", "用户到药店或门店自取"),

    /**
     * 快递配送
     */
    EXPRESS("EXPRESS", "快递配送", "通过第三方快递公司配送"),

    /**
     * 同城配送
     */
    CITY_DELIVERY("CITY_DELIVERY", "同城配送", "由门店或外包骑手进行同城当日达"),

    /**
     * 药店自送
     */
    DRUG_STORE_DELIVERY("DRUG_STORE_DELIVERY", "药店自送", "药店自有配送员负责派送"),

    /**
     * 冷链配送
     */
    COLD_CHAIN("COLD_CHAIN", "冷链配送", "温控运输药品，如疫苗或特殊处方药"),

    /**
     * 智能药柜取药
     */
    PHARMACY_PICKUP_LOCKER("PHARMACY_PICKUP_LOCKER", "智能药柜取药", "用户扫码到智能药柜自助取药");

    /**
     * 枚举值
     */
    private final String type;

    /**
     * 中文名称
     */
    private final String name;

    /**
     * 描述信息
     */
    private final String description;

    DeliveryTypeEnum(String type, String name, String description) {
        this.type = type;
        this.name = name;
        this.description = description;
    }

    /**
     * 根据 code 获取枚举
     */
    public static DeliveryTypeEnum fromCode(String type) {
        if (type == null) {
            return null;
        }
        for (DeliveryTypeEnum deliveryTypeEnum : values()) {
            if (deliveryTypeEnum.type.equals(type)) {
                return deliveryTypeEnum;
            }
        }
        return null;
    }

    /**
     * 兼容旧的整型编码，按枚举声明顺序进行匹配。
     *
     * @param legacyCode 旧的整型编码
     * @return 对应的配送方式枚举
     */
    public static DeliveryTypeEnum fromLegacyCode(Integer legacyCode) {
        if (legacyCode == null) {
            return null;
        }
        for (DeliveryTypeEnum type : values()) {
            if (type.ordinal() == legacyCode) {
                return type;
            }
        }
        return null;
    }

}
