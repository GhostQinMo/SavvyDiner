package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.HotData;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.hmdp.utils.RedisConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    //使用线程池来更新缓存
    private final ExecutorService executorService= Executors.newFixedThreadPool(10);

    //第四次优化：缓存通过商品id查询的商铺信息
    // 第四次优化：1.注入StringRedisTemplate

    @Resource
    StringRedisTemplate stringRedisTemplate;

    @Override
    public Result selectById(Long id) {
        //缓存穿透
//        return CachePenetrate(id);

        /*//使用互斥锁解决缓存击穿
        Result result = CacheBreakdown(id);
        if (result == null) {
            log.info("查询结果为null");
            return Result.fail("空指针异常");
        }*/


        //使用逻辑过期解决缓存击穿
        Result result = CacheBreakdown_expireLogic(id);
        if (result == null) {
            log.info("查询结果为null");
            return Result.fail("空指针异常");
        }
        return result;
    }

    //提取缓存穿透
    private  Result CachePenetrate(Long id ){
        // 2. id是唯一的，所以是根据商铺id为key来保存商品信息的，这里试着采用json字符串形式来保存商铺信息，这里是只是尝试，可以使用map数据结构的
        String shopjson = stringRedisTemplate.opsForValue().get(RedisConstants.CACHE_SHOP_KEY +id.toString());

        //3. 如果命中则直接返回商铺信息
        if (!StrUtil.isBlankIfStr(shopjson)){
            final Shop shop = JSONUtil.toBean(shopjson, Shop.class);
            log.info("从缓存中拿去商铺信息：商铺id为{}",id);
            return Result.ok(shop);
        }
        //解决缓存穿透：缓存空串，因为StrUtil.isBlankIfStr(shopjson)会把""也判断为null,所以这里需要再次判断是否为空，只有真的为null采取查询数据库

        if (!Objects.equals(shopjson, null)) {
            return  Result.fail("发生缓存穿透，请输入正确的查询条件");
        }

        //4. 没有命中，这根据id在数据库中查询商铺信息
        final Shop shopById = getById(id);

        // 5. 数据库中不存在则返回404 (内容不存在)
        if (!Objects.nonNull(shopById)){
            //解决缓存穿透：缓存空串
            stringRedisTemplate.opsForValue().set(
                    RedisConstants.CACHE_SHOP_KEY+id.toString(),
                    "",
                    RedisConstants.CACHE_NULL_TTL,
                    TimeUnit.MINUTES);
            return  Result.fail("商品不存在");
        }
        //6.存在该商铺，则保存商铺信息到redis，然后返回
        stringRedisTemplate.opsForValue().set(
                RedisConstants.CACHE_SHOP_KEY+id.toString(),
                JSONUtil.toJsonStr(shopById),
                RedisConstants.CACHE_SHOP_TTL,
                TimeUnit.MINUTES
        );     //优化：添加缓存一致性解决方法的兜底方案添加过期时间
        return Result.ok(shopById);
    }

    //提取缓存击穿(而且缓存穿透也包含)
    private Result CacheBreakdown(Long id){
        // 2. id是唯一的，所以是根据商铺id为key来保存商品信息的，这里试着采用json字符串形式来保存商铺信息，这里是只是尝试，可以使用map数据结构的
        String shopjson = stringRedisTemplate.opsForValue().get(RedisConstants.CACHE_SHOP_KEY +id.toString());

        //3. 如果命中则直接返回商铺信息
        if (!StrUtil.isBlankIfStr(shopjson)){
            final Shop shop = JSONUtil.toBean(shopjson, Shop.class);
            log.info("从缓存中拿去商铺信息：商铺id为{}",id);
            return Result.ok(shop);
        }
        //解决缓存穿透：缓存空串，因为StrUtil.isBlankIfStr(shopjson)会把""也判断为null,所以这里需要再次判断是否为空，只有真的为null采取查询数据库
        if (!Objects.equals(shopjson, null)) {
            return  Result.fail("发生缓存穿透，请输入正确的查询条件");
        }

        /*这里是我自己写的，没有使用递归
        //4. 没有命中，尝试获取互斥锁，开始重建缓存
        // TODO 这里可以写成double-check
        while(true){
            //这里为什么不需要考虑线程安全问题？redis为我们保证了
            if (!tryMutexLock(id.toString())){
                try {
                    //获取锁失败则休眠一段时间，然后在尝试获取
                    TimeUnit.MILLISECONDS.sleep(500);
                    // TODO 这里应该可以再去查询缓存如果有了，则直接返回
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }else {
                //命中
                break;
            }
        }*/
         Shop shopById=null;

        try {
            //4. 没有命中，尝试获取互斥锁，开始重建缓存
            //4.1 如果没有获取锁成功则休眠一段时间
            if(!tryMutexLock(id.toString())){
                TimeUnit.MILLISECONDS.sleep(200);
                //再次判断
                return CacheBreakdown(id);
            }
            //4.2 如果获取成功，则进入查询数据库
            //  做duble-check
            shopjson = stringRedisTemplate.opsForValue().get(RedisConstants.CACHE_SHOP_KEY + id);
            if (!StrUtil.isBlankIfStr(shopjson)){
                final Shop shop = JSONUtil.toBean(shopjson, Shop.class);
                log.info("从缓存中拿去商铺信息：商铺id为{}",id);
                return Result.ok(shop);
            }

            //如果duble-check失败，加锁，查询数据库
            shopById = getById(id);
            // 5. 数据库中不存在则返回内容不存在

            //模拟重构key时需要一些复杂的流程
            TimeUnit.MILLISECONDS.sleep(200);

            if (!Objects.nonNull(shopById)){
                //解决缓存穿透：缓存空串
                stringRedisTemplate.opsForValue().set(
                        RedisConstants.CACHE_SHOP_KEY+ id,
                        "",
                        RedisConstants.CACHE_NULL_TTL,
                        TimeUnit.MINUTES);
                return  Result.fail("商品不存在");
            }
            //6.存在该商铺，则保存商铺信息到redis，然后返回
            stringRedisTemplate.opsForValue().set(
                    RedisConstants.CACHE_SHOP_KEY+ id,
                    JSONUtil.toJsonStr(shopById),
                    RedisConstants.CACHE_SHOP_TTL,
                    TimeUnit.MINUTES
            );     //优化：添加缓存一致性解决方法的兜底方案添加过期时间

        } catch (InterruptedException e) {
            log.error("商铺缓存重建失败！！！");
            e.printStackTrace();
        }finally{
            //释放互斥锁
            releaseMutexLock(id.toString());
        }
        //返回
        return Result.ok(shopById);
    }

    /**
     *
     * @param key 互斥锁对象
     * @return boolean
     * @Description 获取互斥锁
     */
    private boolean tryMutexLock(String key){
        final Boolean aBoolean = stringRedisTemplate.opsForValue().setIfAbsent(RedisConstants.LOCK_SHOP_KEY+key, "1", RedisConstants.LOCK_SHOP_TTL, TimeUnit.SECONDS);
        //这里需要注意自动拆箱问题
        //因为flag是包装类,而方法返回的是基本数据类型,包装类是可以为null,但基本数据类型不能为null
        //使用Hutool工具包解决
        return BooleanUtil.isTrue(aBoolean);
    }

    /**
     *
     * @param key 互斥锁对象
     * @return void
     * @Description 释放互斥锁
     */
    private void releaseMutexLock(String key){
        final Boolean ifdel = stringRedisTemplate.delete(RedisConstants.LOCK_SHOP_KEY+key);
    }

    /**
     * 缓存击穿值逻辑过期解决方案
     * @param id
     * @return
     * 这里只演示缓存击穿问题，对应的是热点数据，如果没有在缓存命中的话，说明该数据不是热点数据，直接返回null
     */
    private Result CacheBreakdown_expireLogic(Long id){
        // 2. id是唯一的，所以是根据商铺id为key来保存商品信息的，这里试着采用json字符串形式来保存商铺信息，这里是只是尝试，可以使用map数据结构的
        // 注意：这里返回的是HostData热点数据包装对象的json
        String hotdatajson = stringRedisTemplate.opsForValue().get(RedisConstants.CACHE_SHOP_KEY +id.toString());

        //逻辑过期：1.如果为命中未缓存，则直接返回null(说明查询的数据不是热点数据)
        if (StrUtil.isBlank(hotdatajson)) {
            log.debug("缓冲击穿：查询的不是热点数据");
            return null;
        }

        //逻辑过期：2.命中缓存
        //判断是否逻辑过期
        //3.逻辑未过期，直接返回
        //3.1 获取缓存对象
        final HotData hotData = JSONUtil.toBean(hotdatajson, HotData.class);
         LocalDateTime expireDataLogic = hotData.getExpireDataLogic();

        //这里需要注意hostData,getObject()返回的其实是JSONObject对象
        JSONObject jsonObject = (JSONObject)hotData.getObject();

        //将JSONObject转为Shop对象
        final Shop Hotshop = JSONUtil.toBean(jsonObject, Shop.class);

        //判断是否过期
        if(expireDataLogic.isAfter(LocalDateTime.now())){
            return  Result.ok(Hotshop);
        }

        //4. 逻辑过期
        //4.1 是否成功获取锁
        if (tryMutexLock(id.toString())) {
            //做double-check,检查是否逻辑过期
            final HotData hotData1 = JSONUtil.toBean(
                    stringRedisTemplate.opsForValue().get(RedisConstants.CACHE_SHOP_KEY + id.toString()),
                    HotData.class);
            LocalDateTime expireDataLogic1 = hotData1.getExpireDataLogic();
            if (expireDataLogic1.isAfter(LocalDateTime.now())){
                return Result.ok(JSONUtil.toBean((JSONObject) hotData.getObject(),Shop.class));
            }
            //是，开启独立的线程来重建缓存，然后返回过期数据(这里使用线程池)
            final Future<?> future = executorService.submit(() -> {
                //重建缓存
                addHotShop(id, 20L);
            });
        }
        //否 ，直接返回逻辑过期的数据
        return Result.ok(Hotshop);
    }


    //采用修改数据库，在删除缓存来实现数据一致性问题，因为这是单体应用，所以使用事务即可保证修改数据库和删除缓存的一致性
    @Override
    @Transactional
    public Result updatebyid(Shop shop) {
        //1.如果用户id不存在则直接返回
        final Long id = shop.getId();
        if(StrUtil.isBlankIfStr(id)){
            return Result.fail("商铺id为"+id+"不存在");
        }
        //更新数据库
        final boolean b = updateById(shop);
        //删除缓存
        final Boolean delete = stringRedisTemplate.delete(RedisConstants.CACHE_SHOP_KEY + shop.getId());

        return Result.ok("更新成功");
    }

    /**
     *
     * @param id
     * @param expireSecond
     * @Description 添加热点商铺或者重建缓存
     */
    public void addHotShop(Long id , Long expireSecond){
        //1.从数据库中查询商铺信息,封装数据
         Shop shop = getById(id);
         HotData hotData = new HotData(LocalDateTime.now().plusSeconds(expireSecond), shop);
         //2.保存到redis中(这是另一种解决缓存击穿的方法)  //注意这里使用的key和使用互斥锁解决缓存击穿一样
        stringRedisTemplate.opsForValue().set(
                RedisConstants.CACHE_SHOP_KEY+ id,
                JSONUtil.toJsonStr(hotData)
        );
    }
}
