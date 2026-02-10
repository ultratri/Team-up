package com.teamup.server.modules.newbie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamup.server.modules.newbie.entity.NewbieTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * 新手任务Mapper
 */
@Mapper
public interface NewbieTaskMapper extends BaseMapper<NewbieTask> {
}
