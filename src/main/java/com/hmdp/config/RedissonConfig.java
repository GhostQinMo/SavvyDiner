package com.hmdp.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Black_ghost
 * @title: RedissonConfig
 * @projectName AngelXin_dianping
 * @description :616  An unchanging God  Qin_Love
 * @vesion 1.0.0
 * @CreateDate 2023-06-05 16:51:13
 * @Description redisson客户端配置
 **/
@Configuration
public class RedissonConfig {
   /* @Value("#{redis.address}")
    private String redisAddress;*/


    @Bean
    public RedissonClient getRedisClient(){
        //配置
        Config config =new Config();
        //设置基本配置
        config.useSingleServer().setAddress("redis://192.168.241.128:6379");
        //创建redisson客户端
        return Redisson.create(config);
    }
    //使用redisson的mutiLock(联锁)解决主从一致性带来的锁失效问题

   /* @Bean
    public RedissonClient getRedisClient1(){
        //配置
        Config config =new Config();
        //设置基本配置
        config.useSingleServer().setAddress("redis://192.168.241.128:6479");
        //创建redisson客户端
        return Redisson.create(config);
    }*/

  /*  @Bean
    public RedissonClient getRedisClient2(){
        //配置
        Config config =new Config();
        //设置基本配置
        config.useSingleServer().setAddress("redis://192.168.241.128:6579");
        //创建redisson客户端
        return Redisson.create(config);
    }*/
}
