package com.zhangyichuang.medicine.admin.config;

import com.alibaba.druid.pool.DruidDataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * DruidDataSource 配置
 *
 * @author Chuang
 */
@Configuration
public class DruidDataSourceConfig {

    /**
     * 添加 DruidDataSource 组件到容器中，并绑定属性
     */
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource")
    @ConditionalOnProperty(name = "spring.datasource.type", havingValue = "com.alibaba.druid.pool.DruidDataSource")
    public DataSource druid() {
        return new DruidDataSource();
    }


}
