package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.utils.IdGenerateFactory;
import com.hmdp.utils.SimpleDriRedisLock;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */

@Service
@Slf4j
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    //注入需要的实例
    @Autowired
    private IdGenerateFactory idGenerateFactory;

    @Autowired
    private ISeckillVoucherService seckillVoucherService;


    //注入订单service
    @Autowired
    private IVoucherOrderService voucherOrderService;


    //使用StringRedisTemplate获取分布式锁
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result  seckillVoucher(Long voucherId) {
        // 1.查询优惠券
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        // 2.判断秒杀是否开始
        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
            // 尚未开始
            return Result.fail("秒杀尚未开始！");
        }
        // 3.判断秒杀是否已经结束
        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
            // 尚未开始
            return Result.fail("秒杀已经结束！");
        }
        // 4.判断库存是否充足
        if (voucher.getStock() < 1) {
            // 库存不足
            return Result.fail("库存不足！");
        }
       //1.乐观锁一般用于更新数据的，而这里是添加数据，使用悲观锁，TODO 但是可能还存在其他的解决方案
        //2.并发环境优惠卷订单的创建存在问题，所以需要添加锁
        //3.降低锁的粒度，使用用户的id为作为锁对象，但是存在下面几个问题：
            //3.1、锁的释放需要在事务提交以后在释放
            /*3.2、锁需要相同，但是Long对象可能是由long自动装箱来的，所以每次来的userId可能多是一个新对象，
            所以这里用了toString()方法，但是Long的toString方法底层也是每次创建一个新的String对象返回，所以
            这里用了intern()方法保证了同一个user使用同一个锁，使得锁的粒度降低了
            关于intern()方法在JVM中讲到过
             */
        Long userId = UserHolder.getUser().getId();

        //优化：使用redis完成的分布式锁，不使用synchronized
        /*synchronized(userId.toString().intern()){
            *//*3.3、这里巨坑：这个createVoucherOrder方法在这里调用的是使用的是this来调用的，而不是spring为
            IVoucherOrderService生成的动态代理对象来调用的，而@Transactinal事务是基于spring的这个动态代理对象才能实现的
            所以这里通过AopContext.currentProxy()静态方法得到spring动态代理生成的代理对象
             *//*

        }*/
        //1. 尝试获取锁
        String key="voucherOrder:"+userId;
        SimpleDriRedisLock simpleDriRedisLock=new SimpleDriRedisLock(stringRedisTemplate,key);
        //TODO 这里为了断点测试，key的过期时间设置的久一些
        final boolean isLock = simpleDriRedisLock.tryLock(300);

        //失败
        if (!isLock){
            log.debug("获取锁失败");
            return Result.fail("优惠卷不能重复购买");
        }
        //成功
        try {
            //获取当前类的代理对象
            IVoucherOrderService iVoucherOrderService = (IVoucherOrderService) AopContext.currentProxy();
            //创建订单，使用spring事务（spring事务底层是动态代理）
            return iVoucherOrderService.createVoucherOrder(voucherId);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } finally {
            //释放锁
            simpleDriRedisLock.unlock();
        }
        return null;
    }


    //查询不需要事务的保证，减低事务包括的范围（本来事务添加seckillVoucher()方法上的）
    @Override
    @Transactional
    public Result createVoucherOrder(Long voucherId) {
        //一人一单优化：库存充足的情况下，查询订单是否存在（根据优惠卷id和用户id）
        //得到用户id
        Long userId = UserHolder.getUser().getId();
        //查询：根据优惠卷id和用户id
        final Integer count = voucherOrderService.query()
                .eq("voucher_id", voucherId)
                .eq("user_id", userId).count();
        if (count >0) {
            return Result.fail("一个用户只能获取该优惠卷一次");
        }

        //5，扣减库存
        boolean success = seckillVoucherService.update()
                .setSql("stock= stock -1")
                .eq("voucher_id", voucherId)
                .gt("stock", 0)
                .update();
        if (!success) {
            //扣减库存
            return Result.fail("库存不足！");
        }
        //6.创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        // 6.1.订单id(使用前面写的全局id生成器)
        long orderId = idGenerateFactory.getid("order");
        voucherOrder.setId(orderId);
        // 6.2.用户id
        voucherOrder.setUserId(userId);
        // 6.3.代金券id
        voucherOrder.setVoucherId(voucherId);

        save(voucherOrder);

        return Result.ok(orderId);
    }
}
