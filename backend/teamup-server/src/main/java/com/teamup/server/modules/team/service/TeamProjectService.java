package com.teamup.server.modules.team.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.teamup.server.modules.team.entity.TeamProject;
import java.util.List;

/**
 * 团队项目关联服务接口
 */
public interface TeamProjectService extends IService<TeamProject> {
    
    /**
     * 关联团队和项目
     * @param teamId 团队ID
     * @param projectId 项目ID
     * @return 团队项目关联
     */
    TeamProject associateTeamWithProject(Long teamId, Long projectId);
    
    /**
     * 完成项目
     * @param teamId 团队ID
     * @param projectId 项目ID
     */
    void completeProject(Long teamId, Long projectId);
    
    /**
     * 获取团队的所有项目
     * @param teamId 团队ID
     * @return 团队项目关联列表
     */
    List<TeamProject> getTeamProjects(Long teamId);
    
    /**
     * 获取团队正在进行的项目
     * @param teamId 团队ID
     * @return 进行中的项目列表
     */
    List<TeamProject> getActiveProjects(Long teamId);
    
    /**
     * 检查团队是否正在执行某个项目
     * @param teamId 团队ID
     * @param projectId 项目ID
     * @return 是否正在执行
     */
    boolean isTeamWorkingOnProject(Long teamId, Long projectId);
    
    /**
     * 获取团队已完成的项目数量
     * @param teamId 团队ID
     * @return 已完成项目数量
     */
    int getCompletedProjectCount(Long teamId);
}
