package com.teamup.server.modules.activity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamup.server.modules.activity.entity.TeamActivity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 团队活动Mapper
 */
@Mapper
public interface TeamActivityMapper extends BaseMapper<TeamActivity> {
}
