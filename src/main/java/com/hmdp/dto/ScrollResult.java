package com.hmdp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author Black_ghost
 * @title: ScrollResult
 * @projectName AngelXin_dianping
 * @description :616  An unchanging God  Qin_Love
 * @vesion 1.0.0
 * @CreateDate 2023-06-08 21:54:53
 * @Description 用户分页查询自己的收邮箱，返回给前端的DTO
 **/

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ScrollResult {
    private List<?> list;
    private Long minTime;
    private Integer offset;
}
