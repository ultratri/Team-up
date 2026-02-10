package com.teamup.server.modules.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamup.server.modules.project.entity.ProjectFile;
import org.apache.ibatis.annotations.Mapper;

/**
 * 项目文件Mapper
 */
@Mapper
public interface ProjectFileMapper extends BaseMapper<ProjectFile> {
}
