package com.teamup.server.modules.ecosystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamup.server.modules.ecosystem.entity.Like;
import org.apache.ibatis.annotations.Mapper;

/**
 * 点赞Mapper接口
 */
@Mapper
public interface LikeMapper extends BaseMapper<Like> {
}
