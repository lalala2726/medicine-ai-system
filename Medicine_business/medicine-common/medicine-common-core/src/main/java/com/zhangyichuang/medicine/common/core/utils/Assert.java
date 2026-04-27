package com.zhangyichuang.medicine.common.core.utils;


import com.zhangyichuang.medicine.common.core.enums.ResponseCode;
import com.zhangyichuang.medicine.common.core.exception.ParamException;
import com.zhangyichuang.medicine.common.core.exception.ServiceException;
import org.springframework.lang.NonNull;

/**
 * @author Chuang
 * <p>
 * created on 2025/7/10
 */
public class Assert extends org.springframework.util.Assert {


    private Assert() {
        throw new IllegalStateException("Utility class");
    }


    /**
     * 断言给定条件为真，如果为假则抛出带指定错误消息的 ServiceException。
     * 适用于一般业务逻辑错误。
     *
     * @param condition    要断言的条件。
     * @param errorMessage 条件为假时抛出的错误消息。
     */
    public static void isTrue(boolean condition, String errorMessage) {
        if (!condition) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, errorMessage);
        }
    }

    /**
     * 断言给定条件为真，如果为假则抛出带指定错误消息的 ParamException。
     * 适用于参数校验错误。
     *
     * @param condition    要断言的条件。
     * @param errorMessage 条件为假时抛出的错误消息。
     */
    public static void isParamTrue(boolean condition, String errorMessage) {
        if (!condition) {
            throw new ParamException(ResponseCode.PARAM_ERROR, errorMessage);
        }
    }


    /**
     * 断言给定对象不为 null，如果为 null 则抛出带指定错误消息的 ParamException。
     * 适用于参数不能为空的校验。
     *
     * @param object       要断言不为 null 的对象。
     * @param errorMessage 对象为 null 时抛出的错误消息。
     */
    public static void notNull(Object object, String errorMessage) {
        isParamTrue(object != null, errorMessage);
    }

    /**
     * 断言给定字符串不为 null 且不为空字符串（""），如果为空则抛出带指定错误消息的 ParamException。
     *
     * @param text         要断言不为空的字符串。
     * @param errorMessage 字符串为空时抛出的错误消息。
     */
    public static void notEmpty(String text, String errorMessage) {
        isParamTrue(text != null && !text.isEmpty(), errorMessage);
    }


    /**
     * 断言给定集合不为 null 且不为空，如果为空则抛出带指定错误消息的 ParamException。
     *
     * @param collection   要断言不为空的集合。
     * @param errorMessage 集合为空时抛出的错误消息。
     */
    public static void notEmpty(java.util.Collection<?> collection, @NonNull String errorMessage) {
        isParamTrue(collection != null && !collection.isEmpty(), errorMessage);
    }

    /**
     * 断言给定数组不为 null 且不为空，如果为空则抛出带指定错误消息的 ParamException。
     *
     * @param array        要断言不为空的数组。
     * @param errorMessage 数组为空时抛出的错误消息。
     */
    public static void notEmpty(Object[] array, @NonNull String errorMessage) {
        isParamTrue(array != null && array.length > 0, errorMessage);
    }

    /**
     * 断言给定数值大于0，如果不是则抛出带指定错误消息的 ParamException。
     *
     * @param number       要断言大于0的数值。
     * @param errorMessage 数值不大于0时抛出的错误消息。
     */
    public static void isPositive(Long number, String errorMessage) {
        isParamTrue(number != null && number > 0, errorMessage);
    }

    public static void isPositive(Integer number, String errorMessage) {
        isParamTrue(number != null && number > 0, errorMessage);
    }


    /**
     * 断言给定数值大于等于0，如果不是则抛出带指定错误消息的 ParamException。
     *
     * @param number       要断言大于等于0的数值。
     * @param errorMessage 数值不大于等于0时抛出的错误消息。
     */
    public static void isPositiveOrZero(Long number, String errorMessage) {
        isParamTrue(number != null && number >= 0, errorMessage);
    }

    /**
     * 断言两个对象相等，如果不想等则抛出带指定错误消息的 ServiceException。
     * 适用于业务逻辑中两个值必须相等的情况。
     *
     * @param expected     期望的值。
     * @param actual       实际的值。
     * @param errorMessage 不相等时抛出的错误消息。
     */
    public static void equals(Object expected, Object actual, String errorMessage) {
        isTrue(java.util.Objects.equals(expected, actual), errorMessage);
    }
}
