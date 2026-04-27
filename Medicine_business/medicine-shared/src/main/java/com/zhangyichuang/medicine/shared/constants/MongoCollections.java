package com.zhangyichuang.medicine.shared.constants;

/**
 * MongoDB集合名称常量类
 * 定义系统中使用的所有MongoDB集合名称
 *
 * @author Chuang
 */
public final class MongoCollections {

    /**
     * 地址区域集合
     * 存储全国省市区街道信息
     */
    public static final String REGIONS = "regions";

    private MongoCollections() {
        // 工具类,禁止实例化
    }
}
