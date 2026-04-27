package com.zhangyichuang.medicine.common.mongodb.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.DbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

/**
 * MongoDB配置类
 * 配置MongoTemplate、自定义转换器和审计功能
 *
 * @author Chuang
 */
@Configuration
@EnableMongoAuditing
public class MongoConfig {

    /**
     * 配置MongoTemplate
     * 自定义转换器,移除_class字段
     *
     * @param mongoDatabaseFactory MongoDB数据库工厂
     * @param context              MongoDB映射上下文
     * @return MongoTemplate实例
     */
    @Bean
    public MongoTemplate mongoTemplate(MongoDatabaseFactory mongoDatabaseFactory, MongoMappingContext context) {
        DbRefResolver dbRefResolver = new DefaultDbRefResolver(mongoDatabaseFactory);
        MappingMongoConverter converter = new MappingMongoConverter(dbRefResolver, context);

        // 移除_class字段,避免在MongoDB文档中存储Java类型信息
        converter.setTypeMapper(new DefaultMongoTypeMapper(null));

        return new MongoTemplate(mongoDatabaseFactory, converter);
    }
}
