package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IBlogService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.hmdp.utils.RedisConstants.BLOG_LIKED_PERFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

    @Resource
    private IUserService userService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryHotBlog(Integer current) {
        // 根据用户查询
        Page<Blog> page = query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        // 查询用户
//        records.forEach(this::getBlogUser);  //由于逻辑有两步了，所以这里不能用方法引用了
        records.forEach((blog )->{
            //在blog添加用户信息
            this.getBlogUser(blog);

            //判断是否点过赞
            this.isLiked(blog);
        } );
        return Result.ok(records);
    }

    private void getBlogUser(Blog blog) {
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
    }


    @Override
    public Result queryBlogByID(Long id) {
        //根据id获取blog
        final Blog blog = getById(id);
        if (Objects.isNull(blog)) {
            return Result.fail("blog不存在或者已删除");
        }
        //在blog中添加一些用户信息
        getBlogUser(blog);
        //3. 实现点赞功能：通过Blog的isLike字段
        isLiked(blog);
        //返回blog信息
        return Result.ok(blog);

    }

    private void isLiked(Blog blog) {
        // 1.获取登录用户
        final UserDTO user = UserHolder.getUser();
        //判断用户是否空，因为在用户没有登入的时候也是可以查看blog的
        if (user == null) {
            // 用户未登录，无需查询是否点赞
            return;
        }
        final Long userId = user.getId();
        // 2.判断当前登录用户是否已经点赞
        String key = "blog:liked:" + blog.getId();
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        //下面的逻辑是使用set作为点赞人的容器，而后面改为了使用Zset作为点赞人的容器了，因为要做top5
       /* if (BooleanUtil.isTrue(isMember)){
            //如果点过赞了添加高亮
            blog.setIsLike(true);
        }*/
        //优化
        blog.setIsLike(ObjectUtil.isNotNull(score));
    }

    //TODO 这里为什么不需要加锁？

    @Override
    public Result likeBlog(Long id) {

        // 1.获取登录用户
        Long userId = UserHolder.getUser().getId();
        // 2.判断当前登录用户是否已经点赞
        String key = BLOG_LIKED_PERFIX + id;
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        if (score == null) {
            // 3.如果未点赞，可以点赞
            // 3.1.数据库点赞数 + 1
            boolean isSuccess = update().setSql("liked = liked + 1").eq("id", id).update();
            // 3.2.保存用户到Redis的set集合  zadd key value score
            if (isSuccess) {
                stringRedisTemplate.opsForZSet().add(key, userId.toString(), System.currentTimeMillis());
            }
        } else {
            // 4.如果已点赞，取消点赞
            // 4.1.数据库点赞数 -1
            boolean isSuccess = update().setSql("liked = liked - 1").eq("id", id).update();
            // 4.2.把用户从Redis的set集合移除
            if (isSuccess) {
                stringRedisTemplate.opsForZSet().remove(key, userId.toString());
            }
        }
        return Result.ok();
    }

    // TODO 这段逻辑值得反复看
    @Override
    public Result queryBlogLikes(Long id) {
        String key = BLOG_LIKED_PERFIX + id;
        // 1.查询top5的点赞用户 zrange key 0 4
        Set<String> top5 = stringRedisTemplate.opsForZSet().range(key, 0, 4);
        if (top5 == null || top5.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }
        // 2.解析出其中的用户id
        List<Long> ids = top5.stream().map(Long::valueOf).collect(Collectors.toList());
        String idStr = StrUtil.join(",", ids);
        // 3.根据用户id查询用户 WHERE id IN ( 5 , 1 ) ORDER BY FIELD(id, 5, 1)
        /**
         *  这里如果直接使用 userService.listByIds(ids)会出现错误，因为listByIds方法使用的sql语句是wehere id in (ids),而
         *  这个判断是根据ids中的大小来查询的，而不是我们保存的时间戳，所以不可以使用userService.listByIds(ids)
         */

        List<UserDTO> userDTOS = userService.query()
                .in("id", ids).last("ORDER BY FIELD(id," + idStr + ")").list()
                .stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());
        // 4.返回
        return Result.ok(userDTOS);
    }
}
