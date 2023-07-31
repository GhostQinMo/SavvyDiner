package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.utils.IdGenerateFactory;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.ORDER_LOCK_PREFIX;
import static com.hmdp.utils.RedisConstants.SECKILL_ORDER_KEY;


/**
 * @author Black_ghost
 * @title: VoucherOrderServiceImpl
 * @projectName AngelXin_dianping
 * @description :616  An unchanging God  Qin_Love
 * @vesion 1.0.0
 * @CreateDate 2023-06-06 23:01:10
 * @Description 秒杀优惠卷下单
 **/

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

    //使用redisson来获取分布式锁

    @Resource
    private RedissonClient getRedisClient;

    @Resource
    private RedissonClient getRedisClient1;

    @Resource
    private RedissonClient getRedisClient2;


    /*@Override
    public Result  seckillVoucher_old(Long voucherId) {
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
            *//*3.2、锁需要相同，但是Long对象可能是由long自动装箱来的，所以每次来的userId可能多是一个新对象，
            所以这里用了toString()方法，但是Long的toString方法底层也是每次创建一个新的String对象返回，所以
            这里用了intern()方法保证了同一个user使用同一个锁，使得锁的粒度降低了
            关于intern()方法在JVM中讲到过
             *//*
        Long userId = UserHolder.getUser().getId();

        //优化：使用redis完成的分布式锁，不使用synchronized
        *//*synchronized(userId.toString().intern()){
     *//**//*3.3、这里巨坑：这个createVoucherOrder方法在这里调用的是使用的是this来调用的，而不是spring为
            IVoucherOrderService生成的动态代理对象来调用的，而@Transactinal事务是基于spring的这个动态代理对象才能实现的
            所以这里通过AopContext.currentProxy()静态方法得到spring动态代理生成的代理对象
             *//**//*

        }*//*
        //1. 尝试获取锁
        String key="voucherOrder:"+userId;
//        SimpleDriRedisLock simpleDriRedisLock=new SimpleDriRedisLock(stringRedisTemplate,key);
        //TODO 这里为了断点测试，key的过期时间设置的久一些
//        final boolean isLock = simpleDriRedisLock.tryLock(300);


        //使用redisson分布式客户端来获取锁
        //得到锁实例,根据名字获取锁实例
//        final RLock lock = getRedisClient.getLock(key);

        //使用redisson提供的mutiLock（联锁）解决主从一致性带来的锁失效问题

        final RLock lock = getRedisClient.getLock(key);
        *//*final RLock lock0 = getRedisClient.getLock(key);
        final RLock lock1 = getRedisClient1.getLock(key);
        final RLock lock2 = getRedisClient2.getLock(key);

        final RLock lock = getRedisClient1.getMultiLock(lock0, lock1, lock2);*//*

        //尝试获取锁 （使用分布式锁实现一人一单）
        final boolean isLock =  lock.tryLock();
        //tryLock()方法的第一个参数获取锁的最大等待时间（获取失败可重试），默认值为-1 标识不重试，一单失败立刻返回
        // ，第二参数为锁的过期时间，
        // 第三个参数为单位

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
//            simpleDriRedisLock.unlock();
            //释放分布式锁
            lock.unlock();
        }
        return null;
    }*/


    //优化秒杀业务，重新写的业务逻辑，seckillVoucher_old这个方法是原来的秒杀业务逻辑
    //提前加载脚本
    private static final DefaultRedisScript<Long> SECKILLSCRIPT;

    //提前拿到代理对象，以便异步线程中使用
    //获取当前类的代理对象
    public IVoucherOrderService iVoucherOrderService;

    static {
        SECKILLSCRIPT = new DefaultRedisScript<>();
        //加载外部lua脚本文件
        SECKILLSCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        //设置脚本返回值
        SECKILLSCRIPT.setResultType(Long.class);
    }

    //异步处理线程池
    private static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();


    //在类初始化之后执行，因为当这个类初始化好了之后，随时都是有可能要执行的
//    @PostConstruct
    private void init() {
        SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
    }

    //优化：使用消息队列+redis实现秒杀，提高并发能力（这里的消息队列不再是jVM中的阻塞队列了，而是广泛应用的消息队列框架，但是这里演示使用的是
    // redis5.0新增的消息队列stream类型）

    //队列名字
    private final static  String STRING_NAME="stream.orders";
    private class VoucherOrderHandler implements Runnable {

        @Override
        public void run() {
            while (true) {
                try {
                    // 1.获取消息队列中的订单信息 XREADGROUP GROUP g1 c1 COUNT 1 BLOCK 2000 STREAMS s1 >
                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                            Consumer.from("g1", "c1"),
                            StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
                            StreamOffset.create(STRING_NAME, ReadOffset.lastConsumed())
                    );
                    // 2.判断订单信息是否为空
                    if (list == null || list.isEmpty()) {
                        // 如果为null，说明没有消息，继续下一次循环
                        continue;
                    }

                    //添加的步骤： 解析数据,因为这里是获取一个，所以直接取得索引位为0的消息
                    MapRecord<String, Object, Object> record = list.get(0);
                    Map<Object, Object> value = record.getValue();
                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(value, new VoucherOrder(), true);
                    // 3.创建订单
                    handleVoucherOrder(voucherOrder);
                    // 4.确认消息 XACK    STRING_NAME 表示stream队列的名字
                    stringRedisTemplate.opsForStream().acknowledge(STRING_NAME, "g1", record.getId());
                } catch (Exception e) {
                    log.error("处理订单异常", e);
                    //处理异常消息
                    handlePendingList();
                }
            }
        }


/*  优化：这里使用的JVM中的阻塞队列，而优化之后使用的是消息队列
    //使用jdk自带的阻塞队列
    private BlockingQueue<VoucherOrder> orderTasks = new ArrayBlockingQueue<>(1024 * 1024);
    // 当初始化完毕后，就会去从对列中去拿信息
    private class VoucherOrderHandler implements Runnable {

        @Override
        public void run() {
            while (true) {
                try {
                    // 1.获取队列中的订单信息
                    VoucherOrder voucherOrder = orderTasks.take();  //task()阻塞直到队列中有
                    // 2.创建订单
                    handleVoucherOrder(voucherOrder);
                } catch (Exception e) {
                    log.error("处理订单异常", e);
                }
            }
        }*/

        private void handleVoucherOrder(VoucherOrder voucherOrder) {
            //1.获取用户
            Long userId = voucherOrder.getUserId();

            String key = ORDER_LOCK_PREFIX + userId;

            // 2.创建锁对象
            RLock redisLock = getRedisClient.getLock(key);

            // 3.尝试获取锁    //TODO 其实这里可以不需要锁了，因为在lua中保证了，这里加锁是为了兜底
            boolean isLock = redisLock.tryLock();

            // 4.判断是否获得锁成功
            if (!isLock) {
                // 获取锁失败，直接返回失败或者重试
                log.error("不允许重复下单！");
                return;
            }
            try {
                //注意：由于是spring的事务是放在threadLocal中，此时的是多线程，事务会失效
                iVoucherOrderService.createVoucherOrder(voucherOrder);
            } finally {
                // 释放锁
                redisLock.unlock();
            }
        }

        private void handlePendingList() {
            while (true) {
                try {
                    // 1.获取pending-list中的订单信息 XREADGROUP GROUP g1 c1 COUNT 1 BLOCK 2000 STREAMS s1 0
                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                            Consumer.from("g1", "c1"),
                            StreamReadOptions.empty().count(1),
                            StreamOffset.create(STRING_NAME, ReadOffset.from("0"))  //0表示从pedding-list中获取为确认的消息
                    );
                    // 2.判断订单信息是否为空（一般进入到这里的多是有异常的）
                    if (list == null || list.isEmpty()) {
                        // 如果为null，说明没有异常消息，结束循环
                        break;
                    }
                    // 解析数据
                    MapRecord<String, Object, Object> record = list.get(0);
                    Map<Object, Object> value = record.getValue();
                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(value, new VoucherOrder(), true);
                    // 3.创建订单
                    createVoucherOrder(voucherOrder);
                    // 4.确认消息 XACK   STRING_NAME 表示stream队列的名字
                    stringRedisTemplate.opsForStream().acknowledge(STRING_NAME, "g1", record.getId());
                } catch (Exception e) {
                    log.error("处理pendding订单异常", e);
                    try {
                        TimeUnit.MILLISECONDS.sleep(50);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }


    @Override
    public Result seckillVoucher(Long voucherId) {

        //1.执行lua脚本（逻辑包括：库存是否充足，用户是否以购买）

        //用户id
        final Long userid = UserHolder.getUser().getId();
        //1. 生成订单id
        final long seckillOrderId = idGenerateFactory.getid(SECKILL_ORDER_KEY);

        //3. 给代理对象赋值，因为spring的事务需要代理对象（这一步需要提前，不然很可能异步线程在获取这个实例的时候出现空指针异常）
        iVoucherOrderService = (IVoucherOrderService) AopContext.currentProxy();

        final Long res = stringRedisTemplate.execute(
                SECKILLSCRIPT,
                Collections.emptyList(),
                voucherId.toString(), userid.toString(), seckillOrderId + ""
        );

        // 这里res不可能返回null，所以没有关系，r 的值在每个线程内部是独立的，不会存在并发问题。
        final int r = res.intValue();
        if (r != 0) {
            return Result.fail(r == 1 ? "库存不足" : "此卷仅限每人一张");
        }

        //如果下单成功，则将优惠卷id、用户id、订单id保存到阻塞队列



        //4.返回订单id
        return Result.ok(seckillOrderId);

    }


    //这里查询不需要事务的保证，减低事务包括的范围（本来事务添加seckillVoucher()方法上的）
    @Override
    @Transactional
    public void createVoucherOrder(VoucherOrder voucherOrder) {

        // 秒杀优化：使用jdk自带的阻塞队列+redis实现异步优惠卷下单

        Long userId = voucherOrder.getUserId();
        // 5.1.查询订单
        int count = query().eq("user_id", userId).eq("voucher_id", voucherOrder.getVoucherId()).count();
        // 5.2.判断是否存在
        if (count > 0) {
            // 用户已经购买过了
            log.error("用户已经购买过了");
            return;
        }

        // 6.扣减库存
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1") // set stock = stock - 1
                .eq("voucher_id", voucherOrder.getVoucherId()).gt("stock", 0) // where id = ? and stock > 0
                .update();
        if (!success) {
            // 扣减失败
            log.error("库存不足");
            return;
        }
        save(voucherOrder);

    }
}
