package com.teamup.server.modules.team.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamup.server.modules.team.entity.TaskAssignee;
import org.apache.ibatis.annotations.Mapper;

/**
 * 任务负责人Mapper
 * Requirements: 1.1, 1.2
 */
@Mapper
public interface TaskAssigneeMapper extends BaseMapper<TaskAssignee> {
}
