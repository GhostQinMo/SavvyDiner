package com.hmdp;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * @author 20952
 */
//必须暴露代理类,因为在处理事务的时候需要从IOC容器拿到代理类
@EnableAspectJAutoProxy(exposeProxy = true)
@MapperScan("com.hmdp.mapper")
@SpringBootApplication
public class AngelxindianpingApplication {
    public static void main(String[] args) {
        SpringApplication.run(AngelxindianpingApplication.class, args);
    }
}
