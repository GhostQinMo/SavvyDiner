package com.hmdp;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author 20952
 */
@MapperScan("com.hmdp.mapper")
@SpringBootApplication
public class AngelxindianpingApplication {
    public static void main(String[] args) {
        SpringApplication.run(AngelxindianpingApplication.class, args);
    }
}
