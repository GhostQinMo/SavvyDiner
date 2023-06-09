package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.Blog;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IBlogService extends IService<Blog> {

    /**
     * 查询指定页的blog数据
     * @param current Integer
     * @return Result
     */
    Result queryHotBlog(Integer current);

    /**
     * 根据BlogID查询Blog
     * @param id
     * @return
     */
    Result queryBlogByID(Long id);

    /**
     * 基于redis的set集合实现
     * 同一个用户只能点赞一次，再次点击则取消点赞,如果当前用户已经点赞，则点赞按钮高亮显示（前端已实现，判断字段Blog类的isLike属性）
     * @param id
     * @return
     */
    Result likeBlog(Long id);

    /**
     * 基于redis的zset做top5的点赞
     * @param id
     * @return
     */
    Result queryBlogLikes(Long id);

    /**
     *保存blog，并把当前blog发送给当前用户的粉丝
     * @param blog
     * @return
     */
    Result saveBlog(Blog blog);


    /**
     * 分页查询用户关注的博主的新消息，从自己的邮箱中
     * @param max
     * @param offset
     * @return
     */
    Result queryBlogOfFollow(Long max, Integer offset);
}
