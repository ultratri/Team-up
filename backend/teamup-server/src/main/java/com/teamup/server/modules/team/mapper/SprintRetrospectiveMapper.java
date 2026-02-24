package com.teamup.server.modules.team.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamup.server.modules.team.entity.SprintRetrospective;
import org.apache.ibatis.annotations.Mapper;

/**
 * Sprint回顾会议 Mapper接口
 */
@Mapper
public interface SprintRetrospectiveMapper extends BaseMapper<SprintRetrospective> {
}
