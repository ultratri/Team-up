package com.teamup.server.modules.user.service;

import com.teamup.server.modules.user.dto.UserAvailabilityRequest;
import com.teamup.server.modules.user.vo.UserAvailabilityVO;

/**
 * 用户可用性服务接口
 */
public interface UserAvailabilityService {
    
    /**
     * 获取用户的组队意向
     * @param userId 用户ID
     * @return 用户可用性信息
     */
    UserAvailabilityVO getUserAvailability(Long userId);
    
    /**
     * 更新用户的组队意向
     * @param userId 用户ID
     * @param request 组队意向请求
     */
    void updateAvailability(Long userId, UserAvailabilityRequest request);
}
