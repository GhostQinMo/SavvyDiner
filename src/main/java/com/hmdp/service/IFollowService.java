package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.Follow;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IFollowService extends IService<Follow> {

    /**
     * 关注用户
     * @param followUserId 被关注者的id
     * @param isFollow 是否关注
     * @return
     */
    Result follow(Long followUserId, Boolean isFollow);

    /**
     * 取消关注(查看是否关注用户)
     * @param followUserId  被关注者的id
     * @return
     */
    Result isFollow(Long followUserId);

    /**
     * //查询与当前用户的共同关注
     * @param userid
     * @return
     */
    Result commonsUser(Long userid);
}
