package com.teamup.server.modules.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamup.server.modules.project.entity.Project;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 项目Mapper
 */
@Mapper
public interface ProjectMapper extends BaseMapper<Project> {
    
    /**
     * 查询热门项目（按浏览量降序）
     * @param limit 查询数量
     * @return 热门项目列表
     */
    @Select("SELECT * FROM projects WHERE status = 'RECRUITING' ORDER BY views DESC LIMIT #{limit}")
    List<Project> selectHotProjects(int limit);


    /**
     * 查询用户可见的项目ID列表（用户创建的 + 用户所在团队的项目）
     * @param userId 用户ID
     * @return 项目ID列表
     */
    @Select("SELECT DISTINCT p.id FROM projects p " +
            "LEFT JOIN team_projects tp ON p.id = tp.project_id " +
            "LEFT JOIN team_members tm ON tp.team_id = tm.team_id " +
            "WHERE p.creator_id = #{userId} OR tm.user_id = #{userId}")
    List<Long> selectVisibleProjectIds(Long userId);

}

