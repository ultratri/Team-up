package com.teamup.server.modules.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamup.server.modules.user.entity.UserAvailability;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户可用时间Mapper
 */
@Mapper
public interface UserAvailabilityMapper extends BaseMapper<UserAvailability> {
}
