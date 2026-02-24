package com.teamup.server.modules.team.service;

import com.teamup.server.modules.team.vo.*;

/**
 * 团队自定义配置服务接口
 */
public interface TeamCustomConfigService {
    
    /**
     * 获取团队自定义配置
     */
    TeamCustomConfigVO getConfig(Long teamId, Long currentUserId);
    
    /**
     * 更新团队自定义配置
     */
    void updateConfig(Long teamId, Long currentUserId, TeamCustomConfigVO config);
    
    /**
     * 初始化团队配置（创建团队时调用）
     */
    void initConfig(Long teamId);
}
