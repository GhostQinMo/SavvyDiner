package com.hmdp.interceptor;

import com.hmdp.dto.UserDTO;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        final UserDTO userDTO = UserHolder.getUser();
        if (userDTO == null) {
            //如果用户为空，401"未授权"（Unauthorized）
            response.setStatus(401);
            return false;
        }
        //否则，用户存在
        log.info("用户存在");
        return true;
    }


}
