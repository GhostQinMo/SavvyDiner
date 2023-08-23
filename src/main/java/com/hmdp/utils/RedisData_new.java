package com.hmdp.utils;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author Black_ghost
 * @title: RedisData_new
 * @projectName AngelXin_dianping
 * @description :616  An unchanging God  Qin_Love
 * @vesion 1.0.0
 * @CreateDate 2023-08-22 19:24:35
 * @Description 由于先前redis中的json返回的数组中的字段与RedisData中的属性不同，所有这里重新创建一个改动原来的代码
 **/
@Data
public class RedisData_new {
    private LocalDateTime expireDataLogic;
    private Object object;
}
