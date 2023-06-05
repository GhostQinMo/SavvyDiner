package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * @author Black_ghost
 * @title: SimpleDriRedisLock
 * @projectName AngelXin_dianping
 * @description :616  An unchanging God  Qin_Love
 * @vesion 1.0.0
 * @CreateDate 2023-06-04 22:44:48
 * @Description 使用redis实现的分布式锁
 **/

@Component
public class SimpleDriRedisLock implements ILock {
    //操作redis的RedisTemplate
    private StringRedisTemplate stringRedisTemplate;
    //锁的前缀
    private  final String PREFIX="redisLock:";
    //不同业务使用不同的锁
    private String name;

    /**
     *
     * @param stringRedisTemplate
     * @param name1
     */
    public SimpleDriRedisLock(StringRedisTemplate stringRedisTemplate, String name1) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.name = name1;
    }

    /**
     * 获取redis实现的分布式锁
     * @param timeoutSec 锁持有的超时时间，过期后自动释放* @return true代表获取锁成功; false代表获取锁失败大
     * @return boolean
     */
    @Override
    public boolean tryLock(long timeoutSec) {
        //这里的value使用当前线程id
        final long curThID = Thread.currentThread().getId();

        final Boolean aBoolean = stringRedisTemplate.opsForValue().setIfAbsent(PREFIX + name, Long.toString(curThID), timeoutSec, TimeUnit.SECONDS);
            //第一种方法解决自动装箱问题:使用hutool工具包
//           return BooleanUtil.isTrue(aBoolean);
        //第二种方法使用jdk自带的equals方法
        return Boolean.TRUE.equals(aBoolean);
    }

    @Override
    public void unlock() {
        final Boolean delete = stringRedisTemplate.delete(PREFIX + name);
    }
}
