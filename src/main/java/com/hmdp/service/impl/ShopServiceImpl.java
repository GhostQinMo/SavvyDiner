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

import javax.annotation.Resource;
import java.util.Objects;

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
        //4. 没有命中，这根据id在数据库中查询商铺信息
        final Shop shopById = getById(id);

        // 5. 数据库中不存在则返回404 (内容不存在)
        if (!Objects.nonNull(shopById)){
           return  Result.fail("商品不存在");
        }
        //6.存在该商铺，则保存商铺信息到redis，然后返回
        stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY+id.toString(),JSONUtil.toJsonStr(shopById));
        return Result.ok(shopById);
    }
}
