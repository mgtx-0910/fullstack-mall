package com.example.fullstackmall.service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 文档基础信息。
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI fullstackMallOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("二手电商全栈学习项目 API")
                        .description("以后端学习为重点的单体应用接口文档")
                        .version("0.0.1-SNAPSHOT"));
    }
}
