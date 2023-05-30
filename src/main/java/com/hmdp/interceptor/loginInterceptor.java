package com.hmdp.interceptor;

import cn.hutool.core.bean.BeanUtil;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

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

    // 这里只重写了前置和后缀
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //验证用户是否存在
        final HttpSession session = request.getSession();
         User user = (User)session.getAttribute("user");
         if (user==null){
             log.warn("用户为空");
             //设置响应状态码为401,表示为授权的意思
             response.setStatus(401);
             return false  ;
         }
        log.info("用户信息为："+ user);
         //如果存在而保存到ThreadLocal中，方便后面的业务逻辑使用， 这里使用了自己写的一个工具类
         // 第一次优化：传给前端的用户不需要完整的用户信息，而只需要用户页面需要的信息即可（用户名，icon,id）
        //这里使用hutool的工具包复制对象
        final UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
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
