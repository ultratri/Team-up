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
}

