package com.hmdp.config;

import com.hmdp.interceptor.loginInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author Black_ghost
 * @title: InterceptorConfig
 * @projectName hm-dianping
 * @description :616  An unchanging God  Qin_Love
 * @vesion 1.0.0
 * @CreateDate 2023-05-29 16:04:32
 * @Description 拦截器的自定义配置类
 **/

@Slf4j
@Configuration
public class InterceptorConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //创建自定义拦截器对象
        final loginInterceptor loginInterceptor = new loginInterceptor();
        registry.addInterceptor(loginInterceptor).excludePathPatterns(
                "/shop/**",
                "/voucher/**",
                "/shop-type/**",
                "/upload/**",
                "/blog/hot",
                "/user/code",
                "/user/login"
        ).order(1);

//        WebMvcConfigurer.super.addInterceptors(registry);   通过类名.super.方法可以名可以调用接口中的default方法
    }
}
