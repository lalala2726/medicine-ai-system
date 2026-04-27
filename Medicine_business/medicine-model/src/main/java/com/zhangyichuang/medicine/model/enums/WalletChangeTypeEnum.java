package com.zhangyichuang.medicine.model.enums;

import lombok.Getter;

/**
 * 钱包变动类型枚举
 *
 * @author Chuang
 * created 2025/11/08
 */
@Getter
public enum WalletChangeTypeEnum {

    /**
     * 收入（正数）
     */
    INCOME(1, "收入", "余额增加"),

    /**
     * 支出（负数）
     */
    EXPENSE(2, "支出", "余额减少"),

    /**
     * 冻结
     */
    FREEZE(3, "冻结", "余额冻结"),

    /**
     * 解冻
     */
    UNFREEZE(4, "解冻", "余额解冻");

    /**
     * 变动类型代码
     */
    private final Integer code;

    /**
     * 中文名称
     */
    private final String name;

    /**
     * 描述信息
     */
    private final String description;

    WalletChangeTypeEnum(Integer code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    /**
     * 根据 code 获取枚举
     *
     * @param code 变动类型代码
     * @return 枚举对象
     */
    public static WalletChangeTypeEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (WalletChangeTypeEnum type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 判断是否为收入类型
     *
     * @param code 变动类型代码
     * @return true:收入, false:其他
     */
    public static boolean isIncome(Integer code) {
        return INCOME.code.equals(code);
    }

    /**
     * 判断是否为支出类型
     *
     * @param code 变动类型代码
     * @return true:支出, false:其他
     */
    public static boolean isExpense(Integer code) {
        return EXPENSE.code.equals(code);
    }
}

