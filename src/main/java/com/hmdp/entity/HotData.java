package com.hmdp.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author Black_ghost
 * @title: HotData
 * @projectName AngelXin_dianping
 * @description :616  An unchanging God  Qin_Love
 * @vesion 1.0.0
 * @CreateDate 2023-06-01 17:03:16
 * @Description 热点数据（DTO）
 **/
@Data
@AllArgsConstructor
public class HotData {
    LocalDateTime expireDataLogic;
    Object object;
}
