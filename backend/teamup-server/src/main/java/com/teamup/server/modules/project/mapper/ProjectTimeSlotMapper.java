package com.teamup.server.modules.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamup.server.modules.project.entity.ProjectTimeSlot;
import org.apache.ibatis.annotations.Mapper;

/**
 * 项目时间段Mapper
 */
@Mapper
public interface ProjectTimeSlotMapper extends BaseMapper<ProjectTimeSlot> {
}
