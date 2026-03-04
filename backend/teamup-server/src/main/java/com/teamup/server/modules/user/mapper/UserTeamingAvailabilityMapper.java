package com.teamup.server.modules.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamup.server.modules.user.entity.UserTeamingAvailability;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户组队可用性Mapper
 */
@Mapper
public interface UserTeamingAvailabilityMapper extends BaseMapper<UserTeamingAvailability> {
    
    /**
     * 根据用户ID查询组队可用性
     */
    @Select("SELECT * FROM user_availability WHERE user_id = #{userId}")
    UserTeamingAvailability selectByUserId(@Param("userId") Long userId);
}
