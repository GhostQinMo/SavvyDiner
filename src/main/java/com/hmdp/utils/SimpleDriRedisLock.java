package com.hmdp.utils;


import cn.hutool.core.lang.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.List;
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

@Slf4j
public class SimpleDriRedisLock implements ILock {
    //操作redis的RedisTemplate
    private StringRedisTemplate stringRedisTemplate;
    //锁的前缀
    private  final String PREFIX="redisLock:";
    //不同业务使用不同的锁
    private String name;

    private static final String ID_PREFIX= UUID.randomUUID().toString(true);

    public SimpleDriRedisLock(StringRedisTemplate stringRedisTemplate, String name) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.name = name;
    }

    //提前加载脚本
    private static final DefaultRedisScript<Long> UNLOCKSCRIPT;

    static {
        UNLOCKSCRIPT=new DefaultRedisScript<>();
        //加载外部lua脚本文件
        UNLOCKSCRIPT.setLocation(new ClassPathResource("Unlock.lua"));
        //设置脚本返回值
        UNLOCKSCRIPT.setResultType(Long.class);
    }

    /**
     * 获取redis实现的分布式锁
     * @param timeoutSec 锁持有的超时时间，过期后自动释放* @return true代表获取锁成功; false代表获取锁失败大
     * @return boolean
     */
    @Override
    public boolean tryLock(long timeoutSec) {
        //这里的value使用当前线程id
        //线程id在JVM中是一个递增的数字，而在两个JVM中（也就是多线程环境下），很可能产生线程id一样的情况，所以不能用线程id作为锁的标识
        //优化：使用UUID+线程id作为锁的标识
//        final long curThID = Thread.currentThread().getId();

        final String curThID = ID_PREFIX+Thread.currentThread().getId() ;
        ///curThID+""是为了将其转换为字符串类型
        //final Boolean aBoolean = stringRedisTemplate.opsForValue().setIfAbsent(PREFIX + name, curThID+"", timeoutSec, TimeUnit.SECONDS);

        //String.valueOf(curThID)虽然每次返回一个新的String对象，但是保存还是以字符串形式，所以没有问题
        final Boolean aBoolean = stringRedisTemplate.opsForValue().setIfAbsent(PREFIX + name,curThID, timeoutSec, TimeUnit.SECONDS);

        //第一种方法解决自动装箱问题:使用hutool工具包
        // return BooleanUtil.isTrue(aBoolean);

        //第二种方法使用jdk自带的equals方法
        return Boolean.TRUE.equals(aBoolean);
    }

    /*@Override
    public void unlock() {
        //优化：使用锁标识防止锁误删
        //删锁前判断是否是自己的锁
        final String ThreadID = ID_PREFIX+Thread.currentThread().getId() ;
        final String result = stringRedisTemplate.opsForValue().get(PREFIX + name);
        //如果是自己的锁则释放锁
        if (result.equals(ThreadID)){
            final Boolean delete = stringRedisTemplate.delete(PREFIX + name);
        }
    }*/
    //使用lua脚本保证查询锁和删除锁的原子性
     @Override
    public void unlock() {
        //优化：使用锁标识防止锁误删
        //删锁前判断是否是自己的锁
        //如果是自己的锁则释放锁
        //执行lua脚本（这里使用静态代码块提前加载脚本）
         final String ThreadID = ID_PREFIX+Thread.currentThread().getId();

         final Long result = stringRedisTemplate.execute(UNLOCKSCRIPT,
                 (List<String>) Collections.singleton(PREFIX + name),
                 ThreadID);

         if (result==0){
             log.debug("释放锁失败");
         }
     }

}
