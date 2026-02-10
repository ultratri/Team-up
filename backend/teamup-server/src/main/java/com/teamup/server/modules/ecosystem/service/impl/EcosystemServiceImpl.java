package com.teamup.server.modules.ecosystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.teamup.server.modules.competition.entity.Competition;
import com.teamup.server.modules.competition.mapper.CompetitionMapper;
import com.teamup.server.modules.ecosystem.service.EcosystemService;
import com.teamup.server.modules.ecosystem.vo.EcosystemStatsVO;
import com.teamup.server.modules.project.entity.Project;
import com.teamup.server.modules.project.mapper.ProjectMapper;
import com.teamup.server.modules.user.entity.User;
import com.teamup.server.modules.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 生态广场服务实现类
 */
@Service
@RequiredArgsConstructor
public class EcosystemServiceImpl implements EcosystemService {
    
    private final ProjectMapper projectMapper;
    private final UserMapper userMapper;
    private final CompetitionMapper competitionMapper;
    
    @Override
    public EcosystemStatsVO getStats() {
        EcosystemStatsVO stats = new EcosystemStatsVO();
        
        // 活跃项目数（招募中+进行中）
        LambdaQueryWrapper<Project> projectWrapper = new LambdaQueryWrapper<>();
        projectWrapper.in(Project::getStatus, "RECRUITING", "IN_PROGRESS");
        stats.setProjectCount(projectMapper.selectCount(projectWrapper));
        
        // 在线人才数（所有活跃用户）
        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(User::getStatus, "ACTIVE");
        stats.setTalentCount(userMapper.selectCount(userWrapper));
        
        // 进行中比赛数
        LambdaQueryWrapper<Competition> competitionWrapper = new LambdaQueryWrapper<>();
        competitionWrapper.eq(Competition::getStatus, "PUBLISHED");
        stats.setCompetitionCount(competitionMapper.selectCount(competitionWrapper));
        
        return stats;
    }
}
