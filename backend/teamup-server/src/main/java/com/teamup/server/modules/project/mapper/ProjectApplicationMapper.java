package com.teamup.server.modules.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamup.server.modules.project.entity.ProjectApplication;
import org.apache.ibatis.annotations.Mapper;

/**
 * 项目申请Mapper
 */
@Mapper
public interface ProjectApplicationMapper extends BaseMapper<ProjectApplication> {
}

