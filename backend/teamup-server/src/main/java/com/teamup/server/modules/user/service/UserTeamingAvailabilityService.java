package com.teamup.server.modules.user.service;

import com.teamup.server.modules.user.dto.UpdateUserTeamingAvailabilityDTO;
import com.teamup.server.modules.user.vo.UserTeamingAvailabilityVO;

/**
 * 用户组队可用性服务接口
 */
public interface UserTeamingAvailabilityService {
    
    /**
     * 获取用户的组队可用性信息
     * @param userId 用户ID
     * @return 组队可用性信息
     */
    UserTeamingAvailabilityVO getUserTeamingAvailability(Long userId);
    
    /**
     * 更新用户的组队可用性信息
     * @param userId 用户ID
     * @param dto 更新数据
     */
    void updateUserTeamingAvailability(Long userId, UpdateUserTeamingAvailabilityDTO dto);
}
