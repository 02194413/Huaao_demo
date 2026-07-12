package com.lzp.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.lzp.mapper")
public class MybatisPlusConfig {
}
