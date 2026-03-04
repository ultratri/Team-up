package com.teamup.server.modules.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamup.server.modules.project.entity.TeamApplication;
import org.apache.ibatis.annotations.Mapper;

/**
 * 团队申请Mapper
 */
@Mapper
public interface TeamApplicationMapper extends BaseMapper<TeamApplication> {
}
