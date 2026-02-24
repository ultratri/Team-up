package com.teamup.server.modules.team.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamup.server.modules.team.entity.TeamSetting;
import org.apache.ibatis.annotations.Mapper;

/**
 * 团队设置 Mapper接口
 */
@Mapper
public interface TeamSettingMapper extends BaseMapper<TeamSetting> {
}
