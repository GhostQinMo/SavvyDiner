package com.hmdp;

import lombok.extern.slf4j.Slf4j;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Black_ghost
 * @title: RedissonSentinelTest
 * @projectName AngelXin_dianping
 * @description :616  An unchanging God  Qin_Love
 * @vesion 1.0.0
 * @CreateDate 2023-08-03 16:01:03
 * @Description 基于哨兵模式的Reddison，测试布隆过滤器
 **/
@SpringBootTest
@RunWith(SpringRunner.class)
@Slf4j
@Configuration
public class RedissonSentinelTest {
    /**
     * 测试为什么不能够获取属性
     */
   /* @Value("${spring.redis.sentinel.nodes}")
    private String[] sentinals;
    @Test
    public void test01(){
        for (String sentinal : sentinals) {
            System.out.println(sentinal);
        }
    }*/
    public void test01(){

    }
}
