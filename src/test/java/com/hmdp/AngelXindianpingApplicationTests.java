package com.hmdp;

import com.hmdp.service.impl.ShopServiceImpl;
import com.hmdp.utils.IdGenerateFactory;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@SpringBootTest
@RunWith(SpringRunner.class)
@Slf4j
public class AngelXindianpingApplicationTests {

    //由于还没有搭建服务器客户端，所以这里使用测试上传热点数据
    @Resource
    private ShopServiceImpl shopServiceimpl;

    //TODO 测试前一定需要使用先启动这个测试单元，使得这些热点数据在缓存中，因为代码中使用的是逻辑过期来判断缓存是否过期的，如果不预热数据则查不到数据
    @Test
    public void addHostData(){
        shopServiceimpl.addHotShop(1L,100L);
    }



    //注入全局id生成器
    @Autowired
    IdGenerateFactory idGenerateFactory;

    @Test
    public void IdGeneratorTest(){
         long start = System.currentTimeMillis();
        CountDownLatch downLatch=new CountDownLatch(100);
        ExecutorService service;
        service = Executors.newFixedThreadPool(100);
        Runnable task=()->{
            for (int i = 0; i < 300; i++) {
                long shop = idGenerateFactory.getid("shop");
                System.out.println(shop);
            }
           downLatch.countDown();
        };

        for (int i = 0; i < 100; i++) {
            service.submit(task);
        }

        try {
            downLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

         long end = System.currentTimeMillis();
        System.out.println(end-start);
    }


    //测试redisson提供的mutiLock(联锁)解决主从复制一致性带来的锁失效问题

    @Resource
    private RedissonClient getRedisClient;

    @Resource
    private  RedissonClient getRedisClient1;

    @Resource
    private  RedissonClient getRedisClient2;

    @Test
    public void mutiLockTest(){
        final RLock mutiLock0 = getRedisClient.getLock("mutiLock");
        final RLock mutiLock1 = getRedisClient1.getLock("mutiLock");
        final RLock mutiLock2 = getRedisClient2.getLock("mutiLock");
        final RLock multiLock = getRedisClient.getMultiLock(mutiLock0, mutiLock1, mutiLock2);

        final boolean isLock = multiLock.tryLock();

        try {
            if (isLock){
                log.info("获取锁成功");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                TimeUnit.SECONDS.sleep(1000000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //释放锁
            multiLock.unlock();
        }

    }

}
