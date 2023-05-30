package com.hmdp.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

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
        session.setAttribute("code",code);
        // TODO 这里先省略验证发送功能，后期在做
        return Result.ok();
    }


    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {

        System.out.println("loginForm.toString() = " + loginForm.toString());
        //获取验证码和手机号验证
        final String phone = loginForm.getPhone();

        final boolean phoneInvalid = RegexUtils.isPhoneInvalid(phone);
        if (phoneInvalid) {
            return Result.fail("手机号格式错误");
        }

        final String code_new = loginForm.getCode();

        final Object code_old= session.getAttribute("code");

        if (code_new==null || !code_new.equals(code_old.toString())) {
            return Result.fail("手机号格式错误或者验证码错误");
        }

        // 如果所有验证通过，更具手机号查询用户是否存在，使用的mybatis-plus提供的单表查询功能
        User user = query().eq("phone", phone).one();

        //如果用户存在则保存到session中在返回，如果用户不存在则创建用户保存到session中在返回
        if (user==null){
            user = createUserByPhone(phone);
            final boolean save = save(user);
            if (!save){
                log.error("保存用户失败");
            }
        }
        session.setAttribute("user",user);
        // TODO 前端好像直接跳转到index.html了，而没有发送验证请求
        return Result.ok();
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
