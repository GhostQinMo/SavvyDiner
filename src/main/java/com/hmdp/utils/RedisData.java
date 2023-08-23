package com.hmdp.utils;

import lombok.Data;

import java.time.LocalDateTime;

//redis缓存的数据，包含过期时间和数据
@Data
public class RedisData {
    private LocalDateTime expireTime;
    private Object data;
}
