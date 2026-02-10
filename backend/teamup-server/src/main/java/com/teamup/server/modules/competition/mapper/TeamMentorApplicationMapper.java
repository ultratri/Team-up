package com.teamup.server.modules.competition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamup.server.modules.competition.entity.TeamMentorApplication;
import org.apache.ibatis.annotations.Mapper;

/**
 * 指导老师申请 Mapper
 */
@Mapper
public interface TeamMentorApplicationMapper extends BaseMapper<TeamMentorApplication> {
}

