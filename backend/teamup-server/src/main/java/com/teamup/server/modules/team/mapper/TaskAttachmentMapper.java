package com.teamup.server.modules.team.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamup.server.modules.team.entity.TaskAttachment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 任务附件Mapper
 * Requirements: 4.1, 4.3
 */
@Mapper
public interface TaskAttachmentMapper extends BaseMapper<TaskAttachment> {
}
