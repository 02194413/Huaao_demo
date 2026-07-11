package com.lzp;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@MapperScan("com.lzp.mapper")
@EnableAsync
public class HuaaoDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(HuaaoDemoApplication.class, args);
    }

}
