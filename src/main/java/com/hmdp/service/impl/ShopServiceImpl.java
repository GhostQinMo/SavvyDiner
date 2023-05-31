package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.hmdp.utils.RedisConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Objects;
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

    //第四次优化：缓存通过商品id查询的商铺信息
    // 第四次优化：1.注入StringRedisTemplate

    @Resource
    StringRedisTemplate stringRedisTemplate;

    @Override
    public Result selectById(Long id) {
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
}
