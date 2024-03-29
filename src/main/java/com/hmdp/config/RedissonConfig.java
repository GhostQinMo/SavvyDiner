package com.hmdp.config;

import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
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
@Slf4j
@Configuration
public class RedissonConfig {
   /* @Value("${redis.address}")
    private String redisAddress;*/


    //这里的Redisson是连接的Linux本地的redis，而其他地方的redis是连接主从复制的
    @Bean
    public RedissonClient getRedisClient(){
        //配置
        Config config =new Config();
        //设置基本配置
         SingleServerConfig singleServerConfig = config.useSingleServer();
        singleServerConfig.setPassword("redisAngelXin");
        singleServerConfig.setAddress("redis://47.243.242.192:36790");
        //创建redisson客户端
        return Redisson.create(config);
    }


    /**
     * 使用redisson的mutiLock(联锁)解决主从一致性带来的锁失效问题,即多把锁同时获取成功才能获取到临界资源的访问权限
     */
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

    //TODO 为什么这里注入不了属性？
    /*@Value("${spring.redis.master}")
    private String master;
    @Value("${spring.redis.sentinel.nodes}")
    private List<String> sentinelNodes;
    //配置Redisson连接redis哨兵模式
    @Bean
    public RedissonClient redissonClient(){
        Config config=new Config();
        SentinelServersConfig sentinelServersConfig = config.useSentinelServers();
        sentinelNodes.forEach(sentinelServersConfig::addSentinelAddress);
        sentinelServersConfig.setMasterName(master);
        return Redisson.create(config);
    }*/
}
