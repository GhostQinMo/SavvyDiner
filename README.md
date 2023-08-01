# 点滴留影
![Static Badge](https://img.shields.io/badge/Boot-2.3.2.RELEASE-blue)  ![Static Badge](https://img.shields.io/badge/MySQL-8.0.26-blue)  ![Static Badge](https://img.shields.io/badge/hutool-5.7.17-blue)  ![Static Badge](https://img.shields.io/badge/caffeine-2.8.5-blue)   ![Static Badge](https://img.shields.io/badge/RabbitMQ-5.9.0-blue)


点滴留影是一款高效、智能的Java语言后端大学生线上优惠券抢购APP，致力于为大学生提供周围商家优惠券的便捷线上购买体验。通过该APP，大学生可以轻松享受校园生活中的优惠福利，同时为商家带来更多收益，实现"双赢"局面。项目采用了前后端分离架构，使用Java语言作为后端开发语言，并借助Spring Boot框架搭建强大的核心服务。

## 功能特点

- 优惠券秒杀：用户可以浏览和购买各类优惠券，享受特定时间段的限时抢购活动。
- 达人推荐：优秀用户可以推荐热门商家和商品，帮助其他用户做出购买决策。
- 用户签到：用户可以在商家进行签到，获取积分和奖励，并与好友进行比较。
- 好友互动：用户可以关注好友，查看他们的点评和动态，并进行评论和互动。
- 访问统计：系统会对用户访问量进行统计和分析，提供数据报告和趋势分析。
- 商家搜索：用户可以根据地理位置、分类和关键字搜索附近的商家和优惠券。

## APP界面
用户在指定的时间内抢购有限的商家发布的优惠券，用户可以在优惠券有效时间内提前在APP上预定餐品，用户可以到店用餐也可以打包带走的方式完成交易
<div style="display: flex;">
  <img src="https://cdn.staticaly.com/gh/GhostQinMo/ImageBed@master/redis6/image-20230801150126063.png"  alt="Image 1" style="flex: 1; margin: 5px;">
  <img src="https://cdn.staticaly.com/gh/GhostQinMo/ImageBed@master/redis6/image-20230801150007975.png" alt="Image 2" style="flex: 1; margin: 5px;">
  <img src="https://cdn.staticaly.com/gh/GhostQinMo/ImageBed@master/redis6/image-20230722203607652.png" alt="Image 3" style="flex: 1; margin: 5px;">
</div>


## 高性能分析
![image-20210821080511581](https://cdn.staticaly.com/gh/GhostQinMo/ImageBed@master/redis6/image-20210821080511581.png)

在项目中，我实现了一个高效的多级缓存策略，充分利用请求处理的每个环节，从浏览器端到服务端，以及各个中间层，均设置了缓存，以减轻Tomcat压力，提升服务性能。

1. 对于浏览器访问静态资源时，我充分利用浏览器本地缓存机制，使浏览器能够优先读取本地缓存，从而避免不必要的重复请求，提高资源加载速度。

2. 当访问非静态资源，例如通过ajax查询数据时，我们优先考虑访问服务端。在服务端，我们设置了Nginx本地缓存，以避免频繁访问Tomcat。如果Nginx本地缓存未命中，我们将直接查询Redis缓存，避免了请求经过Tomcat的开销。

3. 当Redis缓存未命中，我们再进行Tomcat的查询。在Tomcat中，我们还设置了JVM进程缓存，进一步减少数据库的访问次数。如果JVM进程缓存未命中，我们才会进行数据库查询。

通过以上多级缓存策略，我们有效地减少了对Tomcat和数据库的请求次数，极大地提升了系统的响应速度和性能。在高并发场景下，这种优化策略能够显著减轻服务器负担，确保系统的稳定性和可靠性。同时，我们也充分考虑了用户体验，通过利用浏览器本地缓存，进一步提高了页面加载速度，为用户提供更加优质的服务。

## 项目监控
通过使用Docker Compose搭建Portainer容器实例，监控所有运行容器实例的健康状态、资源使用情况和性能表现等
![image-20230801155302791](https://cdn.staticaly.com/gh/GhostQinMo/ImageBed@master/redis6/image-20230801155302791.png)

## 贡献

- 如果您发现了 bug 或有任何改进意见，请提交 issue 或者发送 pull 请求。
- 欢迎对项目进行 Fork 和分享。

## 授权许可

[MIT License](https://opensource.org/licenses/MIT)
