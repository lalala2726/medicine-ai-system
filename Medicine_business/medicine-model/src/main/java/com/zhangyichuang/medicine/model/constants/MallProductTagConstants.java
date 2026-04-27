package com.zhangyichuang.medicine.model.constants;

/**
 * 商品标签常量。
 *
 * @author Chuang
 */
public final class MallProductTagConstants {

    /**
     * 已启用状态。
     */
    public static final Integer STATUS_ENABLED = 1;

    /**
     * 已禁用状态。
     */
    public static final Integer STATUS_DISABLED = 0;

    /**
     * 标签类型编码分隔符。
     */
    public static final String TYPE_BINDING_SEPARATOR = ":";

    /**
     * 标签类型编码格式。
     */
    public static final String TYPE_CODE_PATTERN = "^[A-Z][A-Z0-9_]*$";

    private MallProductTagConstants() {
    }
}
