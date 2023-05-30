package com.hmdp.interceptor;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.UserDTO;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author Black_ghost
 * @title: loginInterceptor
 * @projectName hm-dianping
 * @description :616  An unchanging God  Qin_Love
 * @vesion 1.0.0
 * @CreateDate 2023-05-28 23:00:49
 * @Description 用于拦截未授权的用户某些请求创建的拦截器
 **/

@Slf4j
public class loginInterceptor implements HandlerInterceptor {
    //问题：由于登入校验的时候需要使用到stringRedisTemplate来查询redis，但是这个loginInterceptor没有被spring管理，
    //所以该怎么获取这个stringRedisTempate实例呢？  使用构造方法

    private StringRedisTemplate stringRedisTemplate;

    public loginInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    // 这里只重写了前置和后缀
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //第二次优化，使用redis+token完成用户验证
        /*//验证用户是否存在
        final HttpSession session = request.getSession();
         User user = (User)session.getAttribute("user");*/
        //第二次优化：1.从请求中获取token
        final String token = request.getHeader("authorization");
        if (StrUtil.isBlank(token)){
            return false;
        }

        String user_key=RedisConstants.LOGIN_USER_KEY+token;
        // 第二次优化：2.从redis查询用户信息
        final Map<Object, Object> userDTOforMap = stringRedisTemplate.opsForHash().entries(user_key);
        // 还原userDTO
        final UserDTO userDTO = BeanUtil.fillBeanWithMap(userDTOforMap, new UserDTO(), false);
        // 为什么不需要判断用户是存在，因为fillBeanWithMap如果没有完成转换则会抛出异常
        /*if (userDTO == null) {
            log.warn("用户为空");
            //设置响应状态码为401,表示为授权的意思
            response.setStatus(401);
            return false;
        }*/
        log.info("用户信息为：" + userDTO);

        // 注意点：第二次优化：3. 需要在访问了用户是，跟新用户的过期时间
        stringRedisTemplate.expire(user_key, RedisConstants.LOGIN_USER_TTL, TimeUnit.SECONDS);

        //如果存在而保存到ThreadLocal中，方便后面的业务逻辑使用， 这里使用了自己写的一个工具类
        // 第一次优化：传给前端的用户不需要完整的用户信息，而只需要用户页面需要的信息即可（用户名，icon,id）
        //这里使用hutool的工具包复制对象
//        final UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);  //第二次优化注释掉的
//        UserHolder.saveUser(user);   // 修改前的

        UserHolder.saveUser(userDTO);

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        HandlerInterceptor.super.postHandle(request, response, handler, modelAndView);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //结束清除用户信息
        UserHolder.removeUser();
    }
}
