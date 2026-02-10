package com.teamup.server.modules.activity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamup.server.modules.activity.entity.UserActivity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户活动Mapper
 */
@Mapper
public interface UserActivityMapper extends BaseMapper<UserActivity> {
}
