package com.teamup.server.modules.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamup.server.modules.project.entity.TeamApplicationMember;
import org.apache.ibatis.annotations.Mapper;

/**
 * 团队申请成员Mapper
 */
@Mapper
public interface TeamApplicationMemberMapper extends BaseMapper<TeamApplicationMember> {
}
