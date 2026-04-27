package com.zhangyichuang.medicine.agent.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 智能体 OpenAPI(Swagger3) 配置类。
 *
 * @author Chuang
 */
@Configuration
public class OpenAPIConfig {

    /**
     * 所有接口分组。
     */
    @Bean
    public GroupedOpenApi allApi() {
        return GroupedOpenApi.builder()
                .group("所有接口")
                .packagesToScan("com.zhangyichuang.medicine.agent.controller")
                .build();
    }

    /**
     * OpenAPI 主配置。
     */
    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("智能体API接口文档")
                        .description("提供 Admin 与 Client 智能体工具接口定义，便于外部 AI 工具快速集成与调用。")
                        .contact(new Contact()
                                .name("Chuang")
                                .email("example@example.com")
                                .url("https://opensource.org/licenses/apache-2-0")));
    }
}
