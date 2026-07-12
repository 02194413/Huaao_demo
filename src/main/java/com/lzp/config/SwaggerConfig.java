package com.lzp.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI shortUrlOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("短链接生成器 API")
                        .description("基于 Spring Boot 的短链接生成服务，支持短链生成、302 跳转、访问统计")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("蓝志朋")
                                .email("3051097568@qq.com")));
    }
}
