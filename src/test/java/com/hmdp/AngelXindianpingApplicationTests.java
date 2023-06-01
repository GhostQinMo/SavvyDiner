package com.hmdp;

import com.hmdp.service.impl.ShopServiceImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

@SpringBootTest
@RunWith(SpringRunner.class)
public class AngelXindianpingApplicationTests {

    //由于还没有搭建服务器客户端，所以这里使用测试上传热点数据
    @Resource
    private ShopServiceImpl shopServiceimpl;

    @Test
    public void addHostData(){
        shopServiceimpl.addHotShop(1L,100L);
    }

}
