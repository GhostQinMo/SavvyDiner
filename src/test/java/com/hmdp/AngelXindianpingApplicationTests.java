package com.hmdp;

import com.hmdp.service.impl.ShopServiceImpl;
import com.hmdp.utils.IdGenerateFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
@RunWith(SpringRunner.class)
public class AngelXindianpingApplicationTests {

    //由于还没有搭建服务器客户端，所以这里使用测试上传热点数据
    @Resource
    private ShopServiceImpl shopServiceimpl;

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
}
