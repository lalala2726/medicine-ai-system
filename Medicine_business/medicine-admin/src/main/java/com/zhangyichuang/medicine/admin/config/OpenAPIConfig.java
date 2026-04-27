package com.zhangyichuang.medicine.admin.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI(Swagger3)配置类
 *
 * @author Chuang
 */
@Configuration
public class OpenAPIConfig {

    /**
     * 所有接口
     */
    @Bean
    public GroupedOpenApi personal() {
        return GroupedOpenApi.builder()
                .group("所有接口")
                .packagesToScan("com.zhangyichuang.medicine.admin.controller")
                .build();
    }


    /**
     * OpenAPI 主配置
     */
    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .description("提供完整的API接口定义与交互说明，便于快速集成和使用。")
                        .contact(new Contact()
                                .name("Chuang")
                                .email("example@example.com")
                                .name("Apache 2.0")
                                .url("https://opensource.org/licenses/apache-2-0")));
    }
}
