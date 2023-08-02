package com.hmdp.config;

import io.lettuce.core.ReadFrom;
import org.springframework.boot.autoconfigure.data.redis.LettuceClientConfigurationBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Black_ghost
 * @title: RedisSentinelConfig
 * @projectName AngelXin_dianping
 * @description :616  An unchanging God  Qin_Love
 * @vesion 1.0.0
 * @CreateDate 2023-07-31 22:27:57
 * @Description redis的主从复制+setntinel监控
 **/
@Configuration
public class RedisSentinelConfig {
    /**
     * 配置读写分离
     * @return
     */
    @Bean
    public LettuceClientConfigurationBuilderCustomizer clientConfigurationBuilderCustomizer(){
        return clientConfigurationBuilder ->clientConfigurationBuilder.readFrom(ReadFrom.REPLICA_PREFERRED);
    }
}
