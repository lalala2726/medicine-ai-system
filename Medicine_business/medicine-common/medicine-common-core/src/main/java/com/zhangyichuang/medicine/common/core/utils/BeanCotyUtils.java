package com.zhangyichuang.medicine.common.core.utils;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Chuang
 * <p>
 * created on 2025/7/7
 */
public class BeanCotyUtils {

    /**
     * 将 List<T> 转换为 List<V>，使用 BeanUtils 进行属性拷贝
     *
     * @param sourceList  源数据列表
     * @param targetClass 目标类型的 Class
     * @param <T>         源数据类型
     * @param <V>         目标数据类型
     * @return 转换后的目标数据列表
     */
    public static <T, V> List<V> copyListProperties(List<T> sourceList, Class<V> targetClass) {
        List<V> targetList = new ArrayList<>();
        if (sourceList == null || sourceList.isEmpty()) {
            return targetList;
        }
        try {
            for (T source : sourceList) {
                if (source != null) {
                    V target = targetClass.getDeclaredConstructor().newInstance();
                    BeanUtils.copyProperties(source, target);
                    targetList.add(target);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("List 属性拷贝失败", e);
        }
        return targetList;
    }

    /**
     * 将 source 对象的属性复制到一个新的 targetClass 实例中
     *
     * @param source      原对象
     * @param targetClass 目标类 class 对象
     * @return 拷贝后的目标对象
     */
    public static <T, V> V copyProperties(T source, Class<V> targetClass) {
        if (source == null) {
            return null;
        }
        try {
            V target = targetClass.getDeclaredConstructor().newInstance();
            BeanUtils.copyProperties(source, target);
            return target;
        } catch (Exception e) {
            throw new RuntimeException("属性拷贝失败", e);
        }
    }

    /**
     * 将 Page<T> 转换为 List<V>，使用 BeanUtils 进行属性拷贝
     *
     * @param sourceList  源数据列表
     * @param targetClass 目标类型的 Class
     * @param <T>         源数据类型
     * @param <V>         目标数据类型
     * @return 转换后的目标数据列表
     */
    public static <T, V> List<V> copyListProperties(Page<T> sourceList, Class<V> targetClass) {
        return copyListProperties(sourceList.getRecords(), targetClass);
    }
}
