package com.teamup.server.modules.team.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamup.server.modules.team.entity.DailyStandup;
import org.apache.ibatis.annotations.Mapper;

/**
 * 每日站会记录 Mapper接口
 */
@Mapper
public interface DailyStandupMapper extends BaseMapper<DailyStandup> {
}
