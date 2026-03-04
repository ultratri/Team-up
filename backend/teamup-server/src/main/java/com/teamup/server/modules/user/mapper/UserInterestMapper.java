package com.teamup.server.modules.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamup.server.modules.user.entity.UserInterest;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户兴趣Mapper
 */
@Mapper
public interface UserInterestMapper extends BaseMapper<UserInterest> {
}
