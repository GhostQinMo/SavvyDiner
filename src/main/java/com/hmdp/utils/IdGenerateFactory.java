package com.hmdp.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * @author Black_ghost
 * @title: IdGenerateFactory
 * @projectName AngelXin_dianping
 * @description :616  An unchanging God  Qin_Love
 * @vesion 1.0.0
 * @CreateDate 2023-06-02 22:48:21
 * @Description 全局id生成器
 **/

@Component
@Slf4j
public class IdGenerateFactory {

    //全局唯一id使用符号位（1）+时间戳（31）+序列号（32）的long型来表示，其中序列号时redis的自增

    //注入stringredistemplate
    @Autowired
    StringRedisTemplate stringRedisTemplate;

    //1.获取时间戳
    /**
     *  定义一个指定的开始时间的时间戳
      */
    private static final long FLAG_TIMESTAMP=1672531200L;


    /**
     * 用户可以自定义序列号的位数
     */
    private  static final int offset=32;

    /**
     * 自定义id生成器
     * @param prefix
     * @return long
     */
    public long  getid(String prefix){
        //获取31时间戳
        final LocalDateTime now = LocalDateTime.now();
        long nowTimeStamp = now.toEpochSecond(ZoneOffset.UTC);
       long idTimeStamp =nowTimeStamp-FLAG_TIMESTAMP;

        //2. 获取序列号(从redis中生成)
        //以天为单位，为这一天生成的所有id设置一个id_key
        final String dayFormat = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        String idKey ="icr"+prefix+dayFormat;
        //这里返回的是long型，而redis返回的是包装类（Long），所以这里涉及自动拆箱问题，提示会有空指针问题，但是redis保证了不会出现空指针
        final long id_sequence = stringRedisTemplate.opsForValue().increment(idKey);
        //3. 拼接生成全局唯一id
        return idTimeStamp <<offset | id_sequence;
    }
    
    //用于生成一个标准
    /*public static void main(String[] args){
        final LocalDateTime flag = LocalDateTime.of(2023, 1, 1, 0, 0, 0);
        final long timeStamp = flag.toEpochSecond(ZoneOffset.UTC);
        System.out.println(timeStamp);
    }*/
}
