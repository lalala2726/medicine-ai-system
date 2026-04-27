package com.zhangyichuang.medicine.common.core.utils;

import java.util.UUID;

/**
 * @author Chuang
 */
public class UUIDUtils {

    /**
     * 获取一个简单UUID
     *
     * @return 简单UUID
     */
    public static String simple() {
        return UUID.randomUUID().toString().replace("-", "");
    }


    /**
     * 复杂UUID
     */
    public static String complex() {
        String simple = simple();
        // 五位随机数
        int random = (int) (Math.random() * 100000);
        long currentTimeMillis = System.currentTimeMillis();
        return simple + currentTimeMillis + random;
    }
}
