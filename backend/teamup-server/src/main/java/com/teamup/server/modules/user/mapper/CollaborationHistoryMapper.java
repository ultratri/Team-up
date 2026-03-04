package com.teamup.server.modules.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamup.server.modules.user.entity.CollaborationHistory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 协作历史Mapper
 */
@Mapper
public interface CollaborationHistoryMapper extends BaseMapper<CollaborationHistory> {
}
