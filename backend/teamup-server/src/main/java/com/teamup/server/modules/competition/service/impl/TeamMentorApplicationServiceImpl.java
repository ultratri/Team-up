package com.teamup.server.modules.competition.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.teamup.server.modules.competition.entity.TeamMentorApplication;
import com.teamup.server.modules.competition.mapper.TeamMentorApplicationMapper;
import com.teamup.server.modules.competition.service.TeamMentorApplicationService;
import org.springframework.stereotype.Service;

/**
 * 指导老师申请服务实现
 */
@Service
public class TeamMentorApplicationServiceImpl extends ServiceImpl<TeamMentorApplicationMapper, TeamMentorApplication>
        implements TeamMentorApplicationService {
}

