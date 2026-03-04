package com.teamup.server.modules.team.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamup.server.modules.team.entity.TeamInvitation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 团队邀请 Mapper
 */
@Mapper
public interface TeamInvitationMapper extends BaseMapper<TeamInvitation> {
}
