package com.teamup.server.modules.team.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamup.server.modules.team.entity.TaskComment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 任务评论Mapper
 * Requirements: 3.1, 3.2, 3.3
 */
@Mapper
public interface TaskCommentMapper extends BaseMapper<TaskComment> {
}
