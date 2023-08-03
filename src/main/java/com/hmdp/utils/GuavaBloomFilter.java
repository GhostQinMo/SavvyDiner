package com.hmdp.utils;

import com.google.common.base.Charsets;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Black_ghost
 * @title: BloomFilter
 * @projectName AngelXin_dianping
 * @description :616  An unchanging God  Qin_Love
 * @vesion 1.0.0
 * @CreateDate 2023-08-03 17:24:20
 * @Description 布隆过滤器
 **/
@Configuration
public class GuavaBloomFilter {
    @Bean
    public BloomFilter<String> bloomFilter(){
        return  BloomFilter.create(Funnels.stringFunnel(Charsets.UTF_8),100000,0.01);
    }
}
