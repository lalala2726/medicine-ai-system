package com.zhangyichuang.medicine.common.core.utils;

import lombok.Getter;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * Spring工具类
 *
 * @author Chuang
 */
@Component
public class SpringUtils implements ApplicationContextAware {

    /**
     * -- GETTER --
     * 获取applicationContext
     */
    @Getter
    private volatile static ApplicationContext applicationContext;

    /**
     * 通过name获取Bean
     */
    public static Object getBean(String name) {
        return getApplicationContext().getBean(name);
    }

    /**
     * 通过class获取Bean
     */
    public static <T> T getBean(Class<T> clazz) {
        return getApplicationContext().getBean(clazz);
    }

    /**
     * 通过name,以及Clazz返回指定的Bean
     */
    public static <T> T getBean(String name, Class<T> clazz) {
        return getApplicationContext().getBean(name, clazz);
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        SpringUtils.applicationContext = applicationContext;
    }
}
