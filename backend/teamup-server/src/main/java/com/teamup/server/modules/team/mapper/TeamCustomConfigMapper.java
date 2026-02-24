package com.teamup.server.modules.team.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamup.server.modules.team.entity.TeamCustomConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 团队自定义配置 Mapper
 */
@Mapper
public interface TeamCustomConfigMapper extends BaseMapper<TeamCustomConfig> {
}
