package com.hmdp;

import com.hmdp.service.impl.ShopServiceImpl;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class HmDianPingApplicationTests {

    //由于还没有搭建服务器客户端，所以这里使用测试上传热点数据

    @Autowired
    ShopServiceImpl shopServiceimpl;

    @Test
    public void addHostData(){

    }
}
