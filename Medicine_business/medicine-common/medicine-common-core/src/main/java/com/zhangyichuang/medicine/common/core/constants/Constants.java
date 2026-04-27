package com.zhangyichuang.medicine.common.core.constants;

/**
 * @author Chuang
 * <p>
 * created on 2025/8/28
 */
public class Constants {


    /**
     * JSON 序列化白名单
     */
    public static final String[] JSON_WHITELIST_STR = {"org.springframework", "com.zhangyichuang.medicine"};

    /**
     * 账号状态常量
     */
    public static final Integer ACCOUNT_UNLOCK_KEY = 0;


    /**
     * 订单超时时间
     */
    public static final int ORDER_TIMEOUT_MINUTES = 30;


}
