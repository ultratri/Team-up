package com.teamup.server.modules.team.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.teamup.server.common.exception.BusinessException;
import com.teamup.server.modules.team.entity.TeamProject;
import com.teamup.server.modules.team.mapper.TeamProjectMapper;
import com.teamup.server.modules.team.service.TeamProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 团队项目关联服务实现
 */
@Service
@RequiredArgsConstructor
public class TeamProjectServiceImpl extends ServiceImpl<TeamProjectMapper, TeamProject> 
        implements TeamProjectService {
    
    private final TeamProjectMapper teamProjectMapper;
    
    @Override
    @Transactional
    public TeamProject associateTeamWithProject(Long teamId, Long projectId) {
        // 检查是否已经关联
        TeamProject existing = lambdaQuery()
                .eq(TeamProject::getTeamId, teamId)
                .eq(TeamProject::getProjectId, projectId)
                .one();
        
        if (existing != null) {
            throw new BusinessException("团队已经关联了该项目");
        }
        
        // 创建关联
        TeamProject teamProject = new TeamProject();
        teamProject.setTeamId(teamId);
        teamProject.setProjectId(projectId);
        teamProject.setStatus("IN_PROGRESS");
        teamProject.setStartedAt(LocalDateTime.now());
        
        save(teamProject);
        return teamProject;
    }
    
    @Override
    @Transactional
    public void completeProject(Long teamId, Long projectId) {
        TeamProject teamProject = lambdaQuery()
                .eq(TeamProject::getTeamId, teamId)
                .eq(TeamProject::getProjectId, projectId)
                .one();
        
        if (teamProject == null) {
            throw new BusinessException("团队项目关联不存在");
        }
        
        if ("COMPLETED".equals(teamProject.getStatus())) {
            throw new BusinessException("项目已经完成");
        }
        
        teamProject.setStatus("COMPLETED");
        teamProject.setCompletedAt(LocalDateTime.now());
        updateById(teamProject);
    }
    
    @Override
    public List<TeamProject> getTeamProjects(Long teamId) {
        return teamProjectMapper.selectByTeamId(teamId);
    }
    
    @Override
    public List<TeamProject> getActiveProjects(Long teamId) {
        return teamProjectMapper.selectActiveByTeamId(teamId);
    }
    
    @Override
    public boolean isTeamWorkingOnProject(Long teamId, Long projectId) {
        return lambdaQuery()
                .eq(TeamProject::getTeamId, teamId)
                .eq(TeamProject::getProjectId, projectId)
                .eq(TeamProject::getStatus, "IN_PROGRESS")
                .exists();
    }
    
    @Override
    public int getCompletedProjectCount(Long teamId) {
        return teamProjectMapper.countCompletedProjects(teamId);
    }
}
