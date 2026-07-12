package com.lzp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class HuaaoDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(HuaaoDemoApplication.class, args);
    }

}
