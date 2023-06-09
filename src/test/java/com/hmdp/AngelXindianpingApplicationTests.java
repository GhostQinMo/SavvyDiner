package com.hmdp;

import com.hmdp.entity.Shop;
import com.hmdp.service.IShopService;
import com.hmdp.service.impl.ShopServiceImpl;
import com.hmdp.utils.IdGenerateFactory;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.hmdp.utils.RedisConstants.SHOP_GEO_KEY;

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
        for (int i = 2; i <15 ; i++) {
            shopServiceimpl.addHotShop((long) i,100L);
        }
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


    //导入店铺数据到redis中的GEO数据类型中
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private IShopService shopService;

    @Test
    public void uploadShopLocationToRedisGEO(){
        //1.获取所有的商铺信息

        //2. 对商铺根据商铺的类型进行分组，以商铺typeid作为key，商铺的信息作为value保存在一个list中


        //3. 批量上传数据到redis中
        //3.1 但是redis是内存级别的，所有不能在redis保存所有的商铺信息，这里只缓存商铺id和longitude 和latitude ,主要用来做商铺定位

        // 1.查询店铺信息
        List<Shop> list = shopService.list();
        // 2.把店铺分组，按照typeId分组，typeId一致的放到一个集合
        Map<Long, List<Shop>> map = list.stream().collect(Collectors.groupingBy(Shop::getTypeId));
        // 3.分批完成写入Redis
        for (Map.Entry<Long, List<Shop>> entry : map.entrySet()) {
            // 3.1.获取类型id
            Long typeId = entry.getKey();
            String key = SHOP_GEO_KEY + typeId;
            // 3.2.获取同类型的店铺的集合
            List<Shop> value = entry.getValue();
            List<RedisGeoCommands.GeoLocation<String>> locations = new ArrayList<>(value.size());
            // 3.3.写入redis GEOADD key 经度 纬度 member
            for (Shop shop : value) {
                // stringRedisTemplate.opsForGeo().add(key, new Point(shop.getX(), shop.getY()), shop.getId().toString());
                locations.add(new RedisGeoCommands.GeoLocation<>(
                        shop.getId().toString(),
                        new Point(shop.getX(), shop.getY())
                ));
            }
            stringRedisTemplate.opsForGeo().add(key, locations);
        }
    }
}
