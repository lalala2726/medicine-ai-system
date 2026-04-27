package com.zhangyichuang.medicine.client.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 客户端OpenAPI(Swagger3)配置类
 *
 * @author Chuang
 */
@Configuration
public class OpenAPIConfig {

    /**
     * 所有接口
     */
    @Bean
    public GroupedOpenApi allApi() {
        return GroupedOpenApi.builder()
                .group("所有接口")
                .packagesToScan("com.zhangyichuang.medicine.client.controller")
                .build();
    }

    /**
     * OpenAPI 主配置
     */
    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("客户端API接口文档")
                        .description("提供完整的客户端API接口定义，包括商城、订单、用户、AI咨询等功能模块")
                        .contact(new Contact()
                                .name("Chuang")
                                .email("example@example.com")
                                .url("https://opensource.org/licenses/apache-2-0")));
    }
}
