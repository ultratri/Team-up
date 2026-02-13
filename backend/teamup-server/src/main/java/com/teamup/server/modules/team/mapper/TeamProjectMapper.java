package com.teamup.server.modules.team.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamup.server.modules.team.entity.TeamProject;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

/**
 * 团队项目关联Mapper
 */
@Mapper
public interface TeamProjectMapper extends BaseMapper<TeamProject> {
    
    /**
     * 查询团队的所有项目
     * @param teamId 团队ID
     * @return 团队项目关联列表
     */
    @Select("SELECT * FROM team_projects WHERE team_id = #{teamId} ORDER BY started_at DESC")
    List<TeamProject> selectByTeamId(Long teamId);
    
    /**
     * 查询项目的团队
     * @param projectId 项目ID
     * @return 团队项目关联
     */
    @Select("SELECT * FROM team_projects WHERE project_id = #{projectId}")
    TeamProject selectByProjectId(Long projectId);
    
    /**
     * 查询团队正在进行的项目
     * @param teamId 团队ID
     * @return 进行中的项目列表
     */
    @Select("SELECT * FROM team_projects WHERE team_id = #{teamId} AND status = 'IN_PROGRESS'")
    List<TeamProject> selectActiveByTeamId(Long teamId);
    
    /**
     * 查询团队已完成的项目数量
     * @param teamId 团队ID
     * @return 已完成项目数量
     */
    @Select("SELECT COUNT(*) FROM team_projects WHERE team_id = #{teamId} AND status = 'COMPLETED'")
    int countCompletedProjects(Long teamId);
}
