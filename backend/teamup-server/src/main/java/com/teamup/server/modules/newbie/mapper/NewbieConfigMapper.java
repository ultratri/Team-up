package com.teamup.server.modules.newbie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamup.server.modules.newbie.entity.NewbieConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 新手保护配置Mapper
 */
@Mapper
public interface NewbieConfigMapper extends BaseMapper<NewbieConfig> {
}
