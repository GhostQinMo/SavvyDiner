package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


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
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    //1. ioc自动注入StringRedisTemplate

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result sendCode(String phone, HttpSession session) {
        // 发送短信验证码并保存验证码
        // 验证手机号是否规范，使用工具类提供的正则表达式
        final boolean phoneInvalid = RegexUtils.isPhoneInvalid(phone);
        if (phoneInvalid) {
            return Result.fail("手机号格式错误");
        }

        // 如果手机号格式正确，使用hutool工具包生成6位的验证码,保存到session中
        final String code = RandomUtil.randomNumbers(6);
        log.debug("生成的验证码为："+code);
       // session.setAttribute("code",code);   // 由于session共享问题，使用redis+token来做用户验证

        //2. 保存session到redis中，使用的数据结构是string,key为手机号，value为验证码
            // 注意点：1. 如果其他服务也使用手机号在redis中保存信息呢？所以需要给key加一个特定的前缀
                    // 2. value必须设置过期时间

        stringRedisTemplate.opsForValue().set(
                RedisConstants.LOGIN_CODE_KEY+phone,
                code,
                RedisConstants.LOGIN_CODE_TTL, TimeUnit.MINUTES);     // 这里巨坑啊，我把时间单位写成了TimeUnit.SECONDS啊，搞了我几个小时
        log.info("获取保存的验证码：{}",stringRedisTemplate.opsForValue().get(RedisConstants.LOGIN_CODE_KEY + phone));
        // 发送验证码
        // TODO 这里先省略验证发送功能，后期在做
        return Result.ok();
    }


    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {

        log.info("登入表的信息：{}",loginForm.toString());
        //获取验证码和手机号验证
        final String phone = loginForm.getPhone();

        final boolean phoneInvalid = RegexUtils.isPhoneInvalid(phone);
        if (phoneInvalid) {
            log.info("手机号格式错误");
            return Result.fail("手机号格式错误");
        }

        final String code_new = loginForm.getCode();

        // 1.不在从session中获取code了则是redis中
       /* final Object code_old= session.getAttribute("code");

        if (code_new==null || !code_new.equals(code_old.toString())) {
            return Result.fail("手机号格式错误或者验证码错误");
        }*/

         //2. 查询redis，查询验证码是否正确

        final String code_old = stringRedisTemplate.opsForValue().get(RedisConstants.LOGIN_CODE_KEY + phone);

        if (code_new==null || !code_new.equals(code_old.toString())) {
            return Result.fail("手机号格式错误或者验证码错误");
        }

        // 如果所有验证通过，更具手机号查询用户是否存在，使用的mybatis-plus提供的单表查询功能
        User user = query().eq("phone", phone).one();

        // 3. 把用户信息保存在到redis中，使用token进行校验，而不是用session来做验证
        /* //如果用户存在则保存到session中在返回，如果用户不存在则创建用户保存到session中在返回
        if (user==null){
            user = createUserByPhone(phone);
            final boolean save = save(user);
            if (!save){
                log.error("保存用户失败");
            }
        }
        session.setAttribute("user",user);*/
        //4.验证是否存在用户
        if (user==null){
            user = createUserByPhone(phone);
            final boolean save = save(user);
            if (!save){
                log.error("保存用户失败");
            }
        }
        //5. 保存用户到redis中
            //5.1 使用uuid来作为token,
            //5.2 确定保存用户的redis数据结构，保存用户，这里用hash，原因是：hask比json字符串节省空间，可以修改hash中的单个值，
                    //注意：这里的用户信息也需要设置过期时间
            //5.3 放回token给前端（这里前端已经做好了，不需要考虑，前端在下次请求会把token放在请求头中以authorization为key）

        //5.1
        final UUID token = UUID.randomUUID();
        final String user_key=RedisConstants.LOGIN_USER_KEY+token;

        //5.2将User转为UserDTO，并存储
        /*(这里有一个错，就是stringRedisTemplate保存的键和值多是string类型的，而这里的用户id是Long类型的
        stringRedisTemplate无法序列化，可以使用hutool工具包解决)
         */

        final UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        //将UserDTO转为map
        final Map<String, Object> stringObjectMap = BeanUtil.beanToMap(
                userDTO,
                new HashMap<>(),
                CopyOptions.create().setFieldValueEditor((field_name,field_value)->{
                return  field_value.toString();   //参数说明，参数1为hash中的字段名，参数2为字段名对应的value,返回值为自定义自定义值
                })
        );

        // 保存用户信息
        stringRedisTemplate.opsForHash().putAll(
                user_key,
                stringObjectMap
        );
        //设置过期时间,这里为10小时
         stringRedisTemplate.expire(user_key, RedisConstants.LOGIN_USER_TTL, TimeUnit.SECONDS);


        //  前端好像直接跳转到index.html了，而没有发送验证请求,需要修改前端代码，将index.html改为info.html
        //将token返回给客户端
        return Result.ok(token);
    }


    //根据手机号创建用户

    public User createUserByPhone(String phone){
        //创建用户需要生成一个随机的用户名
        final User user = new User();
        user.setPhone(phone);
        final String random_username = RandomUtil.randomString(9);
        user.setNickName("user_"+random_username);
        return user;
    }
}
